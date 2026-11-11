#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
SIGN_MODE=signed sh android/build.sh
python3 - <<'PY'
import hashlib,json,pathlib,xml.etree.ElementTree as E
root=E.parse('android/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
version=root.get(ns+'versionName');code=int(root.get(ns+'versionCode'))
file=pathlib.Path(f'artifacts/qingji-{version}.apk');digest=hashlib.sha256(file.read_bytes()).hexdigest()
metadata=dict(version=version,versionCode=code,url=f'https://github.com/ForXd/slim-app/releases/download/v{version}/{file.name}',bytes=file.stat().st_size,sha256=digest,notes='11月自动切换冬日主题；首页能量卡直接修改每项运动；距离类运动按体重、时间、距离估算，支持手动热量；保存按钮固定显示。')
pathlib.Path('artifacts/update.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
pathlib.Path('artifacts/SHA256SUMS').write_text(f'{digest}  {file.name}\n')
print(json.dumps(metadata,ensure_ascii=False,indent=2))
PY
