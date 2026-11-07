#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
SIGN_MODE=signed sh android/build.sh
python3 - <<'PY'
import hashlib,json,pathlib,xml.etree.ElementTree as E
root=E.parse('android/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
version=root.get(ns+'versionName');code=int(root.get(ns+'versionCode'))
file=pathlib.Path(f'artifacts/qingji-{version}.apk');digest=hashlib.sha256(file.read_bytes()).hexdigest()
metadata=dict(version=version,versionCode=code,url=f'https://github.com/ForXd/slim-app/releases/download/v{version}/{file.name}',bytes=file.stat().st_size,sha256=digest,notes='补齐37种水果与常见主食，饮食输入自动搜索、选择后按克重计算，同餐可继续添加多项。')
pathlib.Path('artifacts/update.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
pathlib.Path('artifacts/SHA256SUMS').write_text(f'{digest}  {file.name}\n')
print(json.dumps(metadata,ensure_ascii=False,indent=2))
PY
