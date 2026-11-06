"""Run against an isolated emulator seeded with UiFixture. ADB and ANDROID_SERIAL select device."""
import os,subprocess,xml.etree.ElementTree as E,re,time,json
adb=[os.environ.get('ADB','adb')]
if os.environ.get('ANDROID_SERIAL'):adb+=['-s',os.environ['ANDROID_SERIAL']]
def shell(*args):return subprocess.check_output(adb+['shell',*args]).decode()
def nodes():
 shell('uiautomator','dump','/sdcard/journal-ui.xml')
 return list(E.fromstring(shell('cat','/sdcard/journal-ui.xml')).iter('node'))
def tap(text,nav=False):
 matches=[n for n in nodes() if (n.get('content-desc')==text if nav else n.get('text')==text or n.get('content-desc')==text)]
 if not matches:raise AssertionError('Missing control: '+text)
 n=matches[0];x,y,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));shell('input','tap',str((x+x2)//2),str((y+y2)//2));time.sleep(.25)
def has(text):return any(text in (n.get('text','')+' '+n.get('content-desc','')) for n in nodes())
def expect(text):assert has(text),text
shell('am','force-stop','cn.qingji.diary');shell('am','start','-n','cn.qingji.diary/.MainActivity');time.sleep(1)
tap('百次',True);expect('1 / 4 个挑战')
tap('下一个');expect('2 / 4 个挑战');expect('已完成 6 次')
# Coordinates come from the observed pager card, not a device-specific screen assumption.
card=next(n for n in nodes() if '自己做饭100次，6次' in n.get('content-desc',''))
x,y,x2,y2=map(int,re.findall(r'\d+',card.get('bounds')))
shell('input','swipe',str(x2-25),str((y+y2)//2),str(x+25),str((y+y2)//2),'450');time.sleep(.6)
expect('3 / 4 个挑战');expect('已完成 0 次')
tap('搜索挑战名称');shell('input','text','walk');shell('input','keyevent','4');expect('1 / 1 个挑战');expect('Walk 100')
tap('搜索挑战名称');shell('input','keyevent','123');shell('input','keyevent','67','67','67','67');shell('input','text','zzzz');shell('input','keyevent','4');expect('没有找到这个挑战')
tap('搜索挑战名称');shell('input','keyevent','123');shell('input','keyevent','67','67','67','67');shell('input','keyevent','4');expect('4 / 4 个挑战')
tap('查看已归档');expect('喝水100次');expect('1 / 1 个挑战')
tap('查看进行中');expect('1 / 4 个挑战')
tap('今日',True);expect('68.2');expect('宫保鸡丁配米饭')
tap('记饮食');expect('拍照 / 相册识餐');shell('input','keyevent','4')
tap('趋势',True);expect('近7天');tap('近30天');expect('近30天')
tap('计划',True);expect('1200');expect('应用更新 · 0.6.0')
print(json.dumps({'result':'PASS','checks':['swipe and snap','next card','case-insensitive search','no match','archive scope','preserved journal','meal menu','trend ranges','1200 kcal plan']},ensure_ascii=False))
