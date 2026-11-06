"""Run only on an isolated emulator seeded with UiFixture. Restores plan/quote/theme after UI checks."""
import os,subprocess,xml.etree.ElementTree as E,re,time,json,datetime,pathlib
adb=[os.environ.get('ADB','adb')]
if os.environ.get('ANDROID_SERIAL'):adb+=['-s',os.environ['ANDROID_SERIAL']]
def shell(*args):return subprocess.check_output(adb+['shell',*args]).decode()
def nodes():
 shell('uiautomator','dump','/sdcard/journal-ui.xml')
 return list(E.fromstring(shell('cat','/sdcard/journal-ui.xml')).iter('node'))
def match(n,text):return n.get('text')==text or n.get('content-desc')==text
def bounds(n):return list(map(int,re.findall(r'\d+',n.get('bounds'))))
def tap_node(n):
 x,y,r,b=bounds(n);assert r>x and b>y,n.attrib
 shell('input','tap',str((x+r)//2),str((y+b)//2));time.sleep(.3)
def tap(text,nav=False):
 n=next((n for n in nodes() if n.get('content-desc')==text if nav),None) if nav else next((n for n in nodes() if match(n,text)),None)
 assert n is not None,'Missing control: '+text
 tap_node(n)
def has(text):return any(text in (n.get('text','')+' '+n.get('content-desc','')) for n in nodes())
def expect(text):assert has(text),text
def scroll(direction='down'):
 n=next(n for n in nodes() if n.get('class')=='android.widget.ScrollView');x,y,r,b=bounds(n);middle=(x+r)//2
 start,end=(y+(b-y)*.78,y+(b-y)*.23) if direction=='down' else (y+(b-y)*.23,y+(b-y)*.78)
 shell('input','swipe',str(middle),str(int(start)),str(middle),str(int(end)),'300');time.sleep(.3)
def seek(text):
 for _ in range(5):
  if has(text):return
  scroll()
 raise AssertionError('Cannot find: '+text)
def input_value(label,value):

 for _ in range(5):
  field=next((n for n in nodes() if n.get('class')=='android.widget.EditText' and n.get('content-desc')==label),None)
  if field is not None:break
  scroll()
 assert field is not None,label
 tap_node(field);shell('input','keyevent','123');shell('input','keyevent',*(['67']*30));shell('input','text',value);shell('input','keyevent','4')
def shot(name):
 if not os.environ.get('UI_SCREENSHOT_DIR'):return
 p=pathlib.Path(os.environ['UI_SCREENSHOT_DIR']);p.mkdir(parents=True,exist_ok=True)
 (p/(name+'.png')).write_bytes(subprocess.check_output(adb+['exec-out','screencap','-p']))
checks=[]
shell('am','force-stop','cn.qingji.diary');shell('am','start','-n','cn.qingji.diary/.MainActivity');time.sleep(1)
expect('靓女');expect('检查更新');tap('检查更新');expect('当前版本 0.7.0');expect('立即检查');shell('input','keyevent','4')
checks+=['renamed brand','visible update entry','manual update panel']
tap('百次',True);expect('1 / 4 个挑战');shot('deck')
tap('下一个');expect('2 / 4 个挑战');expect('已完成 6 次')
card=next(n for n in nodes() if '自己做饭100次，6次' in n.get('content-desc',''));x,y,r,b=bounds(card)
shell('input','swipe',str(r-25),str((y+b)//2),str(x+25),str((y+b)//2),'450');time.sleep(.6)
expect('3 / 4 个挑战');expect('已完成 0 次')
tap('搜索挑战名称');shell('input','text','walk');shell('input','keyevent','4');expect('1 / 1 个挑战');expect('Walk 100')
input_value('搜索挑战名称','zzzz');expect('没有找到这个挑战')
tap('搜索挑战名称');shell('input','keyevent','123');shell('input','keyevent','67','67','67','67');shell('input','keyevent','4');expect('4 / 4 个挑战')
tap('查看已归档');expect('喝水100次');expect('1 / 1 个挑战');tap('查看进行中');expect('1 / 4 个挑战')
checks+=['stack swipe and settle','search and no match','archive scope']
tap('今日',True);expect('68.2');expect('今日能量');shot('home')
tap('记饮食');expect('拍照 / 相册识餐');shell('input','keyevent','4')
seek('日历回顾');tap('日历回顾');expect('周一');shot('calendar')
today=datetime.date.today();yesterday=(today-datetime.timedelta(days=1)).isoformat()
n=next(n for n in nodes() if n.get('content-desc','').startswith(yesterday+'，'));tap_node(n)
expect('回到今天');expect('68.6');expect('当日能量');tap('回到今天');expect('68.2')
checks+=['calendar day review and return today','preserved journal and meal menu']
tap('编辑激励语录');expect('写给自己');input_value('我的激励语录','Keep%sgoing');tap('保存语录');expect('Keep going')
tap('编辑激励语录');tap('恢复默认');expect('慢慢来，每一次都算数。')
checks+=['editable quote save and reset']
tap('趋势',True);expect('近7天');tap('近30天');expect('日均摄入');tap('月度');expect(str(today.year)+'年'+str(today.month)+'月');shot('trends-month')
tap('上一月');tap('下一月');expect(str(today.year)+'年'+str(today.month)+'月')
tap('年度');expect(str(today.year)+'年');expect('月均摄入');shot('trends-year');tap('体重');expect('最近体重');expect('月均体重');tap('上一年');expect(str(today.year-1)+'年');tap('下一年');expect(str(today.year)+'年')
checks+=['monthly and annual trends','previous and next period','annual weight metric']
tap('计划',True);expect('1200');shot('plan-input')
input_value('身高（cm）','165');input_value('体重（kg）','60');input_value('年龄','30')
seek('公式性别');tap('公式性别',True);tap('女性公式');seek('日常活动');tap('日常活动',True);tap('久坐为主')
seek('计算我的计划');tap('计算我的计划');expect('预计维持热量');expect('1584');seek('标准减量');shot('plan-result')
n=next(n for n in nodes() if n.get('content-desc','').startswith('标准减量，'));tap_node(n);seek('保存每日计划');tap('保存每日计划');expect('今日能量')
checks+=['profile calculator','preset preview and save']
# Restore the isolated fixture plan through the same public UI.
tap('计划',True)
for _ in range(2):scroll('up')
seek('直接设置');tap('直接设置');input_value('维持热量（千卡，可留空）','1800');input_value('每日摄入目标（千卡）','1200');tap('保存每日计划');expect('今日能量')
checks+=['custom target below 1500']
tap('计划',True);seek('主题与语录');tap('主题与语录')
n=next(n for n in nodes() if n.get('text','').startswith('自动 · '));tap_node(n);tap('冬日雪花');tap('今日',True);shot('winter-home')
tap('计划',True);seek('主题与语录');tap('主题与语录');tap('冬日雪花');tap('自动随四季');tap('今日',True)
checks+=['manual winter theme and automatic restore']
print(json.dumps({'result':'PASS','checks':checks},ensure_ascii=False))
