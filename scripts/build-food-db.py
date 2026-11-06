"""Build the redistributable offline catalogue from pinned source snapshots.
Usage: python3 scripts/build-food-db.py meals.json USDA_SR.zip output.sqlite
"""
import csv,io,json,sqlite3,sys,zipfile,math
recipes,archive,destination=sys.argv[1:]
db=sqlite3.connect(destination)
db.executescript("DROP TABLE IF EXISTS foods; CREATE TABLE foods(id TEXT PRIMARY KEY,name TEXT,english TEXT,kcal REAL,protein REAL,carbs REAL,fat REAL,portion REAL,source TEXT,ingredients TEXT,priority INTEGER); CREATE INDEX food_name ON foods(name);")
def add(row):
 assert all(math.isfinite(v) and v>=0 for v in row[3:8])
 db.execute("INSERT INTO foods VALUES(?,?,?,?,?,?,?,?,?,?,?)",row)
for m in json.load(open(recipes))['meals']:
 p=m['per100g'];grams=m['servingSize']['amount']
 add(('recipe:'+m['id'],m['name'],'',round(m['kcal']*100/grams,2),p['protein'],p['carbs'],p['fat'],grams,'EveryDay_Food · 社区食谱估值','；'.join(m['ingredients']),0))
z=zipfile.ZipFile(archive)
def rows(name):return csv.DictReader(io.TextIOWrapper(z.open(next(p for p in z.namelist() if p.endswith('/'+name+'.csv'))),encoding='utf-8-sig'))
nut={}
for r in rows('food_nutrient'):
 if r['nutrient_id'] in ['1008','1003','1005','1004']:
  nut.setdefault(r['fdc_id'],{})[r['nutrient_id']]=float(r['amount'])
for r in rows('food'):
 n=nut.get(r['fdc_id'],{})
 if all(k in n for k in ['1008','1003','1005','1004']):
  add(('fdc:'+r['fdc_id'],r['description'],r['description'],*[n[k] for k in ['1008','1003','1005','1004']],100,'USDA SR Legacy · FDC '+r['fdc_id'],'美国标准参考数据库，每100克可食部。',1))
db.commit()
print(db.execute('select priority,count(*) from foods group by priority').fetchall())
db.execute('VACUUM')
db.close()
