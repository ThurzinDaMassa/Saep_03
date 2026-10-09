"""Testes de integração reais. Execute SOMENTE contra uma instância de testes.
Python 3 usa apenas biblioteca padrão. O sistema continua sendo Java + MySQL.
"""
import argparse
import concurrent.futures
import datetime
import http.cookiejar
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--url', default='http://127.0.0.1:8081')
parser.add_argument('--confirmar-banco-teste', action='store_true', required=True)
parser.add_argument('--saida', default='testes/resultado_api.json')
args = parser.parse_args()
results = []
ctx = {}
suffix = str(time.time_ns())[-10:]

class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = ''
    def request(self, path, payload=None, expected=200, token=True):
        headers = {}
        data = None
        if payload is not None:
            data = urllib.parse.urlencode(payload).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
            if token:
                headers['X-CSRF-Token'] = self.csrf
        request = urllib.request.Request(args.url + '/api/' + path, data=data, headers=headers)
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        status = response.code
        body = json.loads(response.read().decode())
        assert status == expected, f'Esperado HTTP {expected}, recebido {status}: {body}'
        return body
    def login(self, email, password):
        response = self.request('login', {'email': email, 'senha': password})
        self.csrf = response['csrf']
        return response

admin, receptionist, mechanic, anonymous = Client(), Client(), Client(), Client()

def case(title):
    def wrap(function):
        code = f'CT{len(results)+1:02}'
        start = time.perf_counter()
        try:
            function()
            result = 'APROVADO'
            evidence = 'Resultado esperado confirmado.'
        except Exception as error:
            result = 'FALHOU'
            evidence = str(error)
        results.append({'id': code, 'cenario': title, 'resultado': result,
                        'evidencia': evidence, 'duracao_ms': round((time.perf_counter()-start)*1000)})
        print(f'{code} {result}: {title}', flush=True)
        return function
    return wrap

@case('API protegida rejeita acesso sem sessão')
def _(): anonymous.request('clientes', expected=401)

@case('Senha inválida não autentica')
def _(): anonymous.request('login', {'email':'admin@torq.local','senha':'errada'}, expected=401)

@case('Login válido cria sessão com cookie HttpOnly e SameSite')
def _():
    response=admin.login('admin@torq.local','Oficina@123')
    assert response['usuario']['perfil']=='administrador'
    assert 'senha_hash' not in response['usuario']
    cookie=next(iter(admin.jar))
    assert cookie.has_nonstandard_attr('HttpOnly') and cookie.get_nonstandard_attr('SameSite')=='Strict'

@case('Alteração sem token CSRF é bloqueada')
def _(): admin.request('clientes',{'nome':'Bloqueado'},expected=403,token=False)

@case('Sessões de atendente e mecânico refletem seus perfis')
def _():
    assert receptionist.login('atendimento@torq.local','Atende@123')['usuario']['perfil']=='atendente'
    assert mechanic.login('mecanico@torq.local','Mecanico@123')['usuario']['perfil']=='mecanico'

@case('Atendente não gerencia usuários e mecânico não cadastra clientes')
def _():
    receptionist.request('usuarios', expected=403)
    mechanic.request('clientes', {'nome':'Sem acesso'}, expected=403)

@case('Cadastro de cliente persiste e pode ser consultado')
def _():
    payload={'nome':'Cliente Teste '+suffix,'documento':'T'+suffix,'telefone':'47999990000','email':'teste@example.com'}
    ctx['client_payload']=payload
    ctx['client']=receptionist.request('clientes',payload)['id']
    assert any(x['id']==ctx['client'] for x in admin.request('clientes'))

@case('Documento duplicado é rejeitado sem duplicar cliente')
def _():
    before=len(admin.request('clientes'))
    admin.request('clientes',ctx['client_payload'],expected=409)
    assert len(admin.request('clientes'))==before

@case('Campos obrigatórios e e-mail inválido são rejeitados')
def _():
    admin.request('clientes',{'nome':''},expected=400)
    admin.request('clientes',dict(ctx['client_payload'],email='sem-arroba'),expected=400)

@case('Alteração de cadastro preserva o identificador')
def _():
    response=receptionist.request('clientes',dict(ctx['client_payload'],id=ctx['client'],nome='Cliente Alterado '+suffix))
    assert response['id']==ctx['client']
    assert next(x for x in admin.request('clientes') if x['id']==ctx['client'])['nome'].startswith('Cliente Alterado')

@case('Veículo exige placa válida e cliente existente')
def _():
    vehicle={'cliente_id':ctx['client'],'placa':'ERRADA','marca':'Fiat','modelo':'Uno','ano':2018,'km':12000}
    admin.request('veiculos',vehicle,expected=400)
    admin.request('veiculos',dict(vehicle,placa='TST1A11',cliente_id=2147483647),expected=404)

def create_vehicle(plate):
    return admin.request('veiculos',{'cliente_id':ctx['client'],'placa':plate,'marca':'Fiat','modelo':'Uno','ano':2018,'km':12000})['id']

@case('Cadastro e atualização de veículo vinculam cliente e quilometragem')
def _():
    # Letras da placa variam a cada execução para tornar o teste repetível.
    num=int(suffix)%17576
    letters=''.join(chr(65+(num//(26**i))%26) for i in [2,1,0])
    ctx['plate']=letters+'1A11'
    ctx['vehicle']=create_vehicle(ctx['plate'])
    admin.request('veiculos',{'id':ctx['vehicle'],'cliente_id':ctx['client'],'placa':ctx['plate'],'marca':'Fiat','modelo':'Uno','ano':2018,'km':12500})
    assert next(v for v in admin.request('veiculos') if v['id']==ctx['vehicle'])['km']==12500

@case('Placa duplicada é rejeitada')
def _(): admin.request('veiculos',{'cliente_id':ctx['client'],'placa':ctx['plate'],'marca':'Fiat','modelo':'Uno','ano':2018,'km':0},expected=409)

@case('Catálogo de serviços rejeita preço negativo e guarda preço válido')
def _():
    admin.request('servicos',{'nome':'Teste','descricao':'Teste','preco':'-1'},expected=400)
    ctx['service']=admin.request('servicos',{'nome':'Serviço '+suffix,'descricao':'Revisão de teste','preco':'50.00'})['id']

@case('Cadastro de peça cria saldo inicial rastreável')
def _():
    ctx['part_payload']={'codigo':'TEST-'+suffix,'nome':'Peça Teste '+suffix,'preco':'20.00','estoque':5,'estoque_minimo':2}
    ctx['part']=admin.request('pecas',ctx['part_payload'])['id']
    moves=[m for m in admin.request('movimentacoes') if m['peca_id']==ctx['part']]
    assert len(moves)==1 and moves[0]['tipo']=='entrada' and moves[0]['saldo_apos']==5

@case('Reposição e saída manual registram usuário, motivo e saldo')
def _():
    admin.request('estoque/movimentar',{'peca_id':ctx['part'],'tipo':'entrada','quantidade':2,'motivo':'Reposição de teste'})
    admin.request('estoque/movimentar',{'peca_id':ctx['part'],'tipo':'saida','quantidade':2,'motivo':'Ajuste de teste'})
    assert next(p for p in admin.request('pecas') if p['id']==ctx['part'])['estoque']==5

@case('Saída manual excessiva não altera o estoque')
def _():
    admin.request('estoque/movimentar',{'peca_id':ctx['part'],'tipo':'saida','quantidade':999,'motivo':'Teste negativo'},expected=409)
    assert next(p for p in admin.request('pecas') if p['id']==ctx['part'])['estoque']==5

@case('Abertura de ordem vincula veículo e responsável')
def _():
    ctx['order']=receptionist.request('ordens',{'veiculo_id':ctx['vehicle'],'responsavel_id':3,'relato':'Revisão de teste'})['id']
    detail=admin.request('ordem?id='+str(ctx['order']))
    assert detail['ordem']['status']=='aberta' and detail['ordem']['veiculo_id']==ctx['vehicle']

@case('Duas ordens ativas para o mesmo veículo são bloqueadas')
def _(): admin.request('ordens',{'veiculo_id':ctx['vehicle'],'responsavel_id':3,'relato':'Duplicada'},expected=409)

@case('Ordem não permite pular diretamente para entregue')
def _(): admin.request('ordem/status',{'ordem_id':ctx['order'],'status':'entregue'},expected=409)

@case('Ordem sem serviço não pode ser concluída')
def _():
    mechanic.request('ordem/status',{'ordem_id':ctx['order'],'status':'em_execucao'})
    mechanic.request('ordem/status',{'ordem_id':ctx['order'],'status':'concluida'},expected=409)

@case('Inclusão de serviço e peça calcula total e baixa estoque')
def _():
    mechanic.request('ordem/servico',{'ordem_id':ctx['order'],'servico_id':ctx['service'],'quantidade':1})
    ctx['part_item']=mechanic.request('ordem/peca',{'ordem_id':ctx['order'],'peca_id':ctx['part'],'quantidade':3})['id']
    detail=admin.request('ordem?id='+str(ctx['order']))
    assert float(detail['ordem']['total'])==110.0
    assert next(p for p in admin.request('pecas') if p['id']==ctx['part'])['estoque']==2

@case('Estoque no mínimo gera alerta no painel')
def _(): assert any(p['id']==ctx['part'] for p in admin.request('dashboard')['alertas'])

@case('Consumo excessivo é rejeitado e não cria item ou movimentação')
def _():
    before=admin.request('ordem?id='+str(ctx['order']))
    move_count=len([m for m in admin.request('movimentacoes') if m['peca_id']==ctx['part']])
    mechanic.request('ordem/peca',{'ordem_id':ctx['order'],'peca_id':ctx['part'],'quantidade':5},expected=409)
    after=admin.request('ordem?id='+str(ctx['order']))
    assert len(before['pecas'])==len(after['pecas'])
    assert len([m for m in admin.request('movimentacoes') if m['peca_id']==ctx['part']])==move_count

@case('Remoção de peça devolve saldo e registra histórico')
def _():
    mechanic.request('ordem/remover-peca',{'ordem_id':ctx['order'],'item_id':ctx['part_item']})
    assert next(p for p in admin.request('pecas') if p['id']==ctx['part'])['estoque']==5
    assert not admin.request('ordem?id='+str(ctx['order']))['pecas']

@case('Mudança no catálogo preserva o preço registrado na ordem')
def _():
    admin.request('servicos',{'id':ctx['service'],'nome':'Serviço '+suffix,'descricao':'Preço atualizado','preco':'99.00'})
    assert float(admin.request('ordem?id='+str(ctx['order']))['servicos'][0]['preco_unitario'])==50.0

@case('Mecânico conclui ordem e atendente confirma entrega')
def _():
    mechanic.request('ordem/status',{'ordem_id':ctx['order'],'status':'concluida'})
    mechanic.request('ordem/status',{'ordem_id':ctx['order'],'status':'entregue'},expected=403)
    receptionist.request('ordem/status',{'ordem_id':ctx['order'],'status':'entregue'})
    assert admin.request('ordem?id='+str(ctx['order']))['ordem']['status']=='entregue'

@case('Ordem entregue bloqueia novos itens e reabertura')
def _():
    mechanic.request('ordem/servico',{'ordem_id':ctx['order'],'servico_id':ctx['service'],'quantidade':1},expected=409)
    admin.request('ordem/status',{'ordem_id':ctx['order'],'status':'em_execucao'},expected=409)

@case('Cancelamento devolve todas as peças uma única vez')
def _():
    order=admin.request('ordens',{'veiculo_id':ctx['vehicle'],'responsavel_id':3,'relato':'Teste cancelamento'})['id']
    mechanic.request('ordem/peca',{'ordem_id':order,'peca_id':ctx['part'],'quantidade':2})
    receptionist.request('ordem/status',{'ordem_id':order,'status':'cancelada'})
    admin.request('ordem/status',{'ordem_id':order,'status':'cancelada'},expected=409)
    assert next(p for p in admin.request('pecas') if p['id']==ctx['part'])['estoque']==5
    assert len(admin.request('ordem?id='+str(order))['pecas'])==1

@case('Consumos simultâneos disputam saldo e impedem estoque negativo')
def _():
    part=admin.request('pecas',{'codigo':'CONC-'+suffix,'nome':'Concorrência','preco':'10.00','estoque':1,'estoque_minimo':0})['id']
    order=admin.request('ordens',{'veiculo_id':ctx['vehicle'],'responsavel_id':3,'relato':'Teste concorrência'})['id']
    other_vehicle=create_vehicle(ctx['plate'][:-1]+'2')
    other_order=admin.request('ordens',{'veiculo_id':other_vehicle,'responsavel_id':3,'relato':'Segunda ordem concorrente'})['id']
    def consume(order_to_use):
        try:
            mechanic.request('ordem/peca',{'ordem_id':order_to_use,'peca_id':part,'quantidade':1})
            return 'ok'
        except AssertionError as error:
            assert 'recebido 409' in str(error)
            return 'sem_estoque'
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        values=list(pool.map(consume,[order,other_order]))
    assert sorted(values)==['ok','sem_estoque']
    assert next(p for p in admin.request('pecas') if p['id']==part)['estoque']==0

@case('Cadastro e desativação de usuário controlam autenticação')
def _():
    mail='teste'+suffix+'@torq.local'
    user=admin.request('usuarios',{'nome':'Usuário Teste','email':mail,'senha':'Teste@123','perfil':'mecanico'})['id']
    temp=Client();temp.login(mail,'Teste@123')
    admin.request('usuarios',{'id':user,'nome':'Usuário Teste','email':mail,'perfil':'mecanico','ativo':0,'senha':''})
    Client().request('login',{'email':mail,'senha':'Teste@123'},expected=401)
    temp.request('clientes',expected=401)

@case('Administrador não pode desativar sua própria conta')
def _(): admin.request('usuarios',{'id':1,'nome':'Administrador','email':'admin@torq.local','perfil':'administrador','ativo':0,'senha':''},expected=409)

@case('Identificador inexistente e valores fracionários inválidos são rejeitados')
def _():
    admin.request('ordem?id=2147483647',expected=404)
    admin.request('estoque/movimentar',{'peca_id':ctx['part'],'tipo':'entrada','quantidade':'1.5','motivo':'inválido'},expected=400)

@case('Texto de injeção SQL é tratado como dado')
def _():
    admin.request('clientes',{'nome':"Teste ' OR 1=1 --",'documento':'SQL'+suffix,'telefone':'4700000000','email':''})
    assert len(admin.request('usuarios'))>=3

@case('Histórico e auditoria identificam as operações executadas')
def _():
    history=admin.request('ordem?id='+str(ctx['order']))['historico']
    assert any('entregue' in h['evento'] for h in history)
    assert all(h['usuario'] and h['criado_em'] for h in history)
    assert any('ordem/' in a['acao'] for a in admin.request('auditoria'))

@case('Logout invalida a sessão')
def _():
    mechanic.request('logout',{})
    mechanic.request('me',expected=401)

output={'data_execucao':datetime.datetime.now().astimezone().isoformat(),'url':args.url,
        'total':len(results),'aprovados':sum(x['resultado']=='APROVADO' for x in results),'casos':results}
Path(args.saida).parent.mkdir(parents=True,exist_ok=True)
Path(args.saida).write_text(json.dumps(output,ensure_ascii=False,indent=2),encoding='utf-8')
print(f"\n{output['aprovados']}/{output['total']} aprovados")
raise SystemExit(0 if output['aprovados']==output['total'] else 1)
