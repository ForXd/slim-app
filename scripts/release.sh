#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
SIGN_MODE=signed sh android/build.sh
python3 - <<'PY'
import hashlib,json,pathlib,xml.etree.ElementTree as E
root=E.parse('android/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
version=root.get(ns+'versionName');code=int(root.get(ns+'versionCode'))
file=pathlib.Path(f'artifacts/qingji-{version}.apk');digest=hashlib.sha256(file.read_bytes()).hexdigest()
metadata=dict(version=version,versionCode=code,url=f'https://github.com/ForXd/slim-app/releases/download/v{version}/{file.name}',bytes=file.stat().st_size,sha256=digest,notes='歪脖树修炼笔记全新树形图标和原生动效；新增百次挑战：100格、备注、撤销和归档；每天检查更新并校验安装包；自定义热量目标支持低于1500 kcal。')
pathlib.Path('artifacts/update.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
pathlib.Path('artifacts/SHA256SUMS').write_text(f'{digest}  {file.name}\n')
print(json.dumps(metadata,ensure_ascii=False,indent=2))
PY
