"""Expose verified SR Legacy fruit records in Chinese; retain original IDs/nutrients."""
import sqlite3,pathlib
mapping={'171688':'苹果','167793':'富士苹果','173944':'香蕉','167765':'西瓜','169092':'哈密瓜（甜瓜参考）','169911':'蜜瓜','169097':'橙子','169917':'脐橙','169105':'橘子（柑橘）','168177':'梨（亚洲梨）','169928':'桃子（黄桃）','169949':'李子','171697':'杏','174683':'葡萄（红提、青提参考）','171719':'樱桃（车厘子）','167762':'草莓','171711':'蓝莓','169910':'芒果','169124':'菠萝（凤梨）','168153':'猕猴桃（奇异果）','168211':'黄金奇异果','169086':'荔枝','169089':'龙眼（鲜桂圆）','168192':'榴莲','169941':'柿子','169926':'木瓜','173044':'番石榴（芭乐）','167746':'柠檬（去皮）','169134':'石榴','169108':'百香果','169913':'桑葚','171715':'杨桃','174687':'菠萝蜜','174673':'西柚','167755':'树莓','173946':'黑莓','168175':'番荔枝（释迦）'}
def apply(path):
 c=sqlite3.connect(path)
 for id,name in mapping.items():
  row=c.execute('select english from foods where id=?',('fdc:'+id,)).fetchone();assert row,id
  c.execute('update foods set name=?,priority=0,ingredients=? where id=?',(name,'水果；每100克可食部，不含果皮果核等不可食部分。品种和成熟度会影响营养值；带“参考”的名称使用同类水果数据。','fdc:'+id))
 c.execute("update foods set name='米饭（熟白米饭）',priority=0,ingredients='主食；每100克熟米饭可食部，不是生米。含水量会影响热量。' where id='fdc:168930'")
 c.commit();assert c.execute('pragma integrity_check').fetchone()[0]=='ok';c.close()
if __name__=='__main__':apply(pathlib.Path(__file__).resolve().parents[1]/'android/data/foods.sqlite');print('PASS:',len(mapping),'Chinese fruit entries')
