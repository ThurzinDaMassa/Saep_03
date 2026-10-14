"""Verifica a inicialização pela raiz, subpasta e configuração explícita/local.
Não conecta ao servidor MySQL: usa o diagnóstico do programa compilado.
"""
import argparse
import datetime
import json
import os
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--java', default='java')
parser.add_argument('--saida', default='testes/resultado_configuracao_organizado.json')
args = parser.parse_args()
project = Path(__file__).resolve().parents[1]
classpath = os.pathsep.join([str(project / 'dist/TorqOficina.jar'), str(project / 'lib/*')])
build = project / 'build'
build.mkdir(exist_ok=True)
results = []

def check(title, cwd, properties, expected, success=True):
    run = subprocess.run([args.java, *properties, '-cp', classpath, 'oficina.App', '--diagnostico'],
                         cwd=cwd, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=15)
    passed = (run.returncode == 0 if success else run.returncode != 0) and expected in (run.stdout + run.stderr)
    results.append({'cenario': title, 'resultado': 'APROVADO' if passed else 'FALHOU'})
    print(results[-1]['resultado'] + ': ' + title)
    if not passed:
        print(run.stdout + run.stderr)

with tempfile.TemporaryDirectory(prefix='configuracao-', dir=build) as directory:
    fixture = Path(directory).resolve()
    assert fixture.is_relative_to(build.resolve())
    base = fixture / 'TorqOficina'
    (base / 'web').mkdir(parents=True)
    (base / 'banco').mkdir()
    config = 'db.engine=mysql\ndb.name={name}\nserver.port=8099\n'
    (base / 'config.properties').write_text(config.format(name='torq_padrao'), encoding='utf-8')
    check('Raiz do repositório localiza TorqOficina', fixture, [], 'torq_padrao')
    check('Execução na pasta TorqOficina usa configuração padrão', base, [], 'torq_padrao')
    (base / 'config.local.properties').write_text(config.format(name='torq_local'), encoding='utf-8')
    check('Configuração local tem prioridade pela raiz', fixture, [], 'torq_local')
    check('Configuração local tem prioridade pela pasta do projeto', base, [], 'torq_local')
    check('Configuração explícita relativa tem prioridade sobre a local', base,
          ['-Dtorq.config=config.properties'], 'torq_padrao')
    check('Configuração explícita absoluta é preservada', fixture,
          ['-Dtorq.config=' + str(base / 'config.properties')], 'torq_padrao')
    check('Diretório torq.root explícito é preservado', fixture,
          ['-Dtorq.root=' + str(base)], 'torq_local')
    check('Diretório inválido informa como corrigir', fixture,
          ['-Dtorq.root=' + str(fixture / 'inexistente')], 'torq.root=CAMINHO', success=False)

output = project / args.saida
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({
    'data_execucao': datetime.datetime.now().astimezone().isoformat(),
    'total': len(results), 'aprovados': sum(r['resultado'] == 'APROVADO' for r in results),
    'casos': results
}, ensure_ascii=False, indent=2), encoding='utf-8')
raise SystemExit(0 if all(r['resultado'] == 'APROVADO' for r in results) else 1)
