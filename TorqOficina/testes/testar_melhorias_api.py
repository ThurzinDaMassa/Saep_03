"""Verifica a edição de atendimento e as permissões adicionadas na revisão.
Execute após testar_api.py, apenas na instância isolada de testes.
"""
import argparse, datetime, http.cookiejar, json, urllib.error, urllib.parse, urllib.request
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--url',default='http://127.0.0.1:8081')
parser.add_argument('--confirmar-banco-teste',action='store_true',required=True)
parser.add_argument('--saida',default='testes/resultado_api.json')
args=parser.parse_args()
class Client:
 def __init__(self,email,password):
  self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=''
  self.csrf=self.request('login',{'email':email,'senha':password})['csrf']
 def request(self,path,payload=None,expected=200,csrf=True):
  headers={};body=None
  if payload is not None:
   body=urllib.parse.urlencode(payload).encode();headers['Content-Type']='application/x-www-form-urlencoded'
   if csrf:headers['X-CSRF-Token']=self.csrf
  request=urllib.request.Request(args.url+'/api/'+path,data=body,headers=headers)
  try:response=self.opener.open(request,timeout=15)
  except urllib.error.HTTPError as e:response=e
  value=json.loads(response.read().decode());assert response.code==expected,(response.code,value);return value
admin=Client('admin@torq.local','Oficina@123')
attendant=Client('atendimento@torq.local','Atende@123')
mechanic=Client('mecanico@torq.local','Mecanico@123')
order=next(o for o in admin.request('ordens') if o['id']==1)
results=[]
def case(title):
 def decorator(function):
  try:function();result='APROVADO';evidence='Resultado esperado confirmado.'
  except Exception as e:result='FALHOU';evidence=str(e)
  results.append({'id':f'CT{37+len(results):02}','cenario':title,'resultado':result,'evidencia':evidence})
  print(results[-1]['id'],result,title,flush=True)
  return function
 return decorator
def payload(**extra):return {'ordem_id':1,'responsavel_id':3,'relato':'Revisão com dados atualizados',**extra}
@case('Administrador edita responsável e relato sem alterar veículo, itens ou saldo')
def _():
 before=admin.request('ordem?id=1');stock=admin.request('pecas')
 admin.request('ordem/dados',payload(responsavel_id=2))
 after=admin.request('ordem?id=1')
 assert after['ordem']['responsavel_id']==2 and after['ordem']['relato']=='Revisão com dados atualizados'
 assert before['ordem']['veiculo_id']==after['ordem']['veiculo_id'] and before['servicos']==after['servicos'] and before['pecas']==after['pecas'] and stock==admin.request('pecas')
@case('Mecânico não pode editar os dados de recepção da ordem')
def _():mechanic.request('ordem/dados',payload(),expected=403)
@case('Atendente pode editar e a alteração registra autor no histórico e auditoria')
def _():
 attendant.request('ordem/dados',payload())
 detail=admin.request('ordem?id=1')
 assert detail['ordem']['responsavel_id']==3
 assert any('Dados do atendimento' in h['evento'] and h['usuario']=='Atendimento' for h in detail['historico'])
 assert any(a['acao'].startswith('ordem/dados') and a['usuario']=='Atendimento' for a in admin.request('auditoria'))
@case('Responsável inexistente rejeita a alteração e preserva o atendimento')
def _():
 before=admin.request('ordem?id=1');admin.request('ordem/dados',payload(responsavel_id=2147483647),expected=404);assert before==admin.request('ordem?id=1')
@case('Relato vazio rejeita alteração sem criar histórico')
def _():
 before=admin.request('ordem?id=1');admin.request('ordem/dados',payload(relato=''),expected=400);assert before==admin.request('ordem?id=1')
@case('Edição de dados exige token CSRF válido')
def _():admin.request('ordem/dados',payload(),expected=403,csrf=False)
@case('Ordem concluída rejeita edição de responsável e relato')
def _():
 admin.request('ordem/status',{'ordem_id':1,'status':'concluida'})
 before=admin.request('ordem?id=1');admin.request('ordem/dados',payload(relato='Não pode mudar'),expected=409);assert before==admin.request('ordem?id=1')
@case('Auditoria administrativa permanece inacessível ao atendente e mecânico')
def _():
 attendant.request('auditoria',expected=403);mechanic.request('auditoria',expected=403)
path=Path(args.saida)
report=json.loads(path.read_text(encoding='utf-8'))
assert report['url']==args.url,'Os resultados precisam ser do mesmo banco de testes.'
report['casos']=[c for c in report['casos'] if int(c['id'][2:])<37]+results
report['total']=len(report['casos']);report['aprovados']=sum(c['resultado']=='APROVADO' for c in report['casos'])
report['data_revisao']=datetime.datetime.now().astimezone().isoformat()
path.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(f"Resultado acumulado: {report['aprovados']}/{report['total']}")
raise SystemExit(0 if all(c['resultado']=='APROVADO' for c in results) else 1)
