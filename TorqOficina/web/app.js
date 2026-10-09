'use strict';
const $=s=>document.querySelector(s);
const icons={dashboard:'<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',ordens:'<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V2h6v2M9 9h6M9 13h6M9 17h3"/>',clientes:'<circle cx="9" cy="7" r="3"/><path d="M3 20v-3a6 6 0 0 1 12 0v3M16 4a3 3 0 0 1 0 6M18 13a5 5 0 0 1 3 5v2"/>',veiculos:'<path d="m5 7 2-4h10l2 4 2 3v8H3v-8zM3 10h18M5 18v3M19 18v3M6 14h2M16 14h2"/>',servicos:'<path d="M14 4a6 6 0 0 0-7 7L2 16l6 6 5-5a6 6 0 0 0 7-7l-4 4-4-4z"/>',pecas:'<path d="m12 3 9 5v9l-9 5-9-5V8zM3 8l9 5 9-5M12 13v9M7 5l9 5"/>',movimentacoes:'<path d="M3 7h17l-4-4M21 17H4l4 4M20 7l-4 4M4 17l4-4"/>',historico:'<path d="M3 11a9 9 0 1 1 1 6M3 4v7h7M12 7v5l3 2"/>',usuarios:'<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',bell:'<path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5zM10 21h4"/>',plus:'<path d="M12 5v14M5 12h14"/>',search:'<circle cx="10" cy="10" r="6"/><path d="m15 15 5 5"/>',arrow:'<path d="M4 12h16M14 6l6 6-6 6"/>',back:'<path d="M20 12H4m6-6-6 6 6 6"/>',check:'<path d="m5 12 4 4 10-10"/>',alert:'<path d="m12 3 10 18H2zM12 9v4M12 17v.5"/>'};
const icon=k=>`<svg viewBox="0 0 24 24" aria-hidden="true">${icons[k]||icons.ordens}</svg>`;
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const brl=v=>Number(v||0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
const dt=v=>v?new Date(String(v).replace(' ','T')).toLocaleString('pt-BR',{dateStyle:'short',timeStyle:'short'}):'—';
const roles={administrador:'Administrador',atendente:'Atendente',mecanico:'Mecânico'};
const statuses={aberta:'Aberta',em_execucao:'Em execução',aguardando_pecas:'Aguardando peças',concluida:'Concluída',entregue:'Entregue',cancelada:'Cancelada'};
const transitions={aberta:['em_execucao','aguardando_pecas','cancelada'],em_execucao:['aguardando_pecas','concluida','cancelada'],aguardando_pecas:['em_execucao','cancelada'],concluida:['entregue'],entregue:[],cancelada:[]};
const eventText=v=>String(v??'').replace(/\b(em_execucao|aguardando_pecas|concluida|entregue|cancelada|aberta)\b/g,k=>statuses[k]);
function auditText(v){const labels={clientes:'Cliente',veiculos:'Veículo',servicos:'Serviço',pecas:'Peça',usuarios:'Usuário',ordens:'Ordem de serviço','ordem/dados':'Dados da ordem','ordem/status':'Situação da ordem','ordem/servico':'Item de serviço','ordem/peca':'Item de peça','ordem/remover-servico':'Remoção de serviço','ordem/remover-peca':'Devolução de peça','estoque/movimentar':'Movimentação de estoque'};return String(v??'').replace(/^(\S+) (registrado|alterado) #(\d+)$/,(_,key,verb,id)=>(labels[key]||key)+' · '+(verb==='alterado'?'Atualização':'Registro')+' #'+id);}
Object.assign(icons,{refresh:'<path d="M20 7v5h-5M4 17v-5h5M5 8a8 8 0 0 1 13-3l2 2M4 17l2 2a8 8 0 0 0 13-3"/>',download:'<path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/>',auditoria:'<path d="m12 2 8 4v7c0 5-8 9-8 9S4 18 4 13V6zM8 12l3 3 5-6"/>',car:'<path d="m20 65 10-25 20-18h64l20 18 12 25v16H14V65zM30 40h104M23 58h18m80 0h16M38 83v7m84-7v7M51 24l-9 16m62-16 9 16M51 69h58"/>'});
const pages=[['dashboard','Visão geral'],['ordens','Ordens de serviço'],['clientes','Clientes'],['veiculos','Veículos'],['servicos','Serviços'],['pecas','Peças e estoque'],['movimentacoes','Movimentações'],['historico','Histórico'],['usuarios','Usuários'],['auditoria','Auditoria']];
const adminPages=new Set(['usuarios','auditoria']);
let user=null,csrf='',data={},route='dashboard',orderId=null,filterStatus='',searchTerm='';
let currentPage=1,pageSize=12,sortKey='',onlyAlerts=false,dateFrom='',dateTo='',navigationVersion=0,refreshVersion=0,requests=0;
let currentOrder=null,formContext=null,formDirty=false,formSaving=false,commandItems=[],commandIndex=0;
const normalize=v=>String(v??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR');
const visiblePages=()=>pages.filter(([k])=>!adminPages.has(k)||user?.perfil==='administrador');
function savedPreferences(key){try{return JSON.parse(sessionStorage.getItem('torq:'+key)||'{}');}catch{return {};}}
function savePreferences(){try{sessionStorage.setItem('torq:'+route,JSON.stringify({searchTerm,filterStatus,currentPage,pageSize,sortKey,onlyAlerts,dateFrom,dateTo}));}catch{/* Navegação também funciona sem armazenamento. */}}
function restorePreferences(){const s=savedPreferences(route);searchTerm=s.searchTerm||'';filterStatus=s.filterStatus||'';currentPage=Math.max(1,Number(s.currentPage)||1);pageSize=[12,24,48].includes(s.pageSize)?s.pageSize:12;sortKey=s.sortKey||'';onlyAlerts=!!s.onlyAlerts;dateFrom=s.dateFrom||'';dateTo=s.dateTo||'';}
function loading(active){requests=Math.max(0,requests+(active?1:-1));$('#request-progress').hidden=requests===0;$('#refresh-data').disabled=requests>0;$('.sync-status').classList.toggle('busy',requests>0);}
async function api(path,payload) {
 const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),15000);
 const options={headers:{},signal:controller.signal,credentials:'same-origin'};
 if(payload!==undefined){options.method='POST';options.headers['Content-Type']='application/x-www-form-urlencoded';options.headers['X-CSRF-Token']=csrf;options.body=new URLSearchParams(payload);}
 loading(true);
 try{const response=await fetch('/api/'+path,options);let body;try{body=await response.json();}catch{throw new Error('O servidor retornou uma resposta inválida. Atualize os dados e tente novamente.');}
  if(!response.ok){if(response.status===401&&path!=='login')showLogin();throw new Error(body.erro||'Não foi possível concluir a operação.');}return body;
 }catch(err){if(err.name==='AbortError')throw new Error('O servidor demorou para responder. Confira a conexão e tente novamente.');if(err instanceof TypeError)throw new Error('Sem conexão com o servidor. Confira se a oficina está em execução.');throw err;}
 finally{clearTimeout(timeout);loading(false);}
}
function toast(message,error=false){const node=document.createElement('div');node.className='toast'+(error?' error-toast':'');node.setAttribute('role',error?'alert':'status');node.innerHTML=icon(error?'alert':'check')+'<span></span><button aria-label="Dispensar aviso">✕</button>';node.querySelector('span').textContent=message;$('#toast-region').append(node);while($('#toast-region').children.length>3)$('#toast-region').firstChild.remove();const dismiss=()=>{node.classList.add('leaving');setTimeout(()=>node.remove(),180);};node.querySelector('button').onclick=dismiss;setTimeout(dismiss,error?7000:4500);}
function showLogin(){user=null;csrf='';data={};navigationVersion++;refreshVersion++;$('#app').hidden=true;$('#login').hidden=false;$('#loading').hidden=true;for(const dialog of document.querySelectorAll('dialog[open]'))dialog.close();formDirty=false;formSaving=false;}
async function refresh(){
 const version=++refreshVersion,identity=user?.id,me=await api('me');if(version!==refreshVersion||!user||user.id!==identity)return;user=me.usuario;csrf=me.csrf;
 const keys=['clientes','veiculos','servicos','pecas','responsaveis','ordens','dashboard','movimentacoes','historico'];if(user.perfil==='administrador')keys.push('usuarios','auditoria');
 const values=await Promise.all(keys.map(k=>api(k)));if(version!==refreshVersion||!user||user.id!==identity||csrf!==me.csrf)return;data=Object.fromEntries(keys.map((k,i)=>[k,values[i]]));
 $('#sync-label').textContent='Atualizado às '+new Date().toLocaleTimeString('pt-BR',{hour:'2-digit',minute:'2-digit'});
}
function allowed(rolesAllowed){return !!user&&rolesAllowed.includes(user.perfil);}
function startUI(){
 if(!user)return;
 $('#app').hidden=false;$('#login').hidden=true;$('#loading').hidden=true;$('#user-name').textContent=user.nome;$('#user-role').textContent=roles[user.perfil];$('#avatar').textContent=user.nome.split(' ').slice(0,2).map(s=>s[0]).join('').toUpperCase();
 $('#today').textContent=new Date().toLocaleDateString('pt-BR',{day:'2-digit',month:'short',year:'numeric'});$('#alert-shortcut').innerHTML=icon('bell')+(data.dashboard.resumo.alertas?'<span class="notification-dot"></span>':'');$('#refresh-data').innerHTML=icon('refresh');document.querySelectorAll('.search-icon').forEach(n=>n.innerHTML=icon('search'));
 $('#navigation').innerHTML=visiblePages().map(([k,n])=>`<button data-nav="${k}" class="${k===route||(k==='ordens'&&route==='ordem')?'active':''}" ${k===route?'aria-current="page"':''}>${icon(k)}<span>${n}</span>${k==='pecas'&&data.dashboard.resumo.alertas?`<span class="nav-count">${data.dashboard.resumo.alertas}</span>`:''}</button>`).join('');
}
async function navigate(page,id,opts={}){
 if(!user)return;savePreferences();if(page!=='ordem'&&!visiblePages().some(([k])=>k===page))page='dashboard';
 route=page;orderId=id?Number(id):null;if(page==='ordem'&&(!Number.isInteger(orderId)||orderId<1)){route='ordens';orderId=null;}
 const version=++navigationVersion;restorePreferences();if(opts.search!==undefined){searchTerm=opts.search;currentPage=1;}if(opts.status!==undefined){filterStatus=opts.status;currentPage=1;}if(opts.alerts!==undefined){onlyAlerts=opts.alerts;currentPage=1;}
 startUI();const hash='#'+route+(route==='ordem'?'/'+orderId:'');if(!opts.fromHash&&location.hash!==hash)history.pushState(null,'',hash);
 await render(version);if(version!==navigationVersion||!user)return;savePreferences();$('#content').classList.remove('entering');void $('#content').offsetWidth;$('#content').classList.add('entering');window.scrollTo({top:0,behavior:'instant'});
}
function routeFromHash(){const [page,id]=location.hash.slice(1).split('/');return navigate(page||'dashboard',id,{fromHash:true});}
window.addEventListener('hashchange',()=>{if(user)routeFromHash().catch(e=>toast(e.message,true));});
const badge=s=>`<span class="badge ${esc(s)}">${esc(statuses[s]||s)}</span>`;
const heading=(title,subtitle,button='')=>`<div class="page-heading"><div><span class="eyebrow">CENTRAL DE OPERAÇÕES</span><h1>${title}</h1><p>${subtitle}</p></div><div class="heading-actions">${button}</div></div>`;
const newButton=(key,label)=>`<button class="primary" data-new="${key}">${icon('plus')} ${label}</button>`;
function empty(cols,text='Nenhum registro encontrado.',hint='Experimente outro termo ou ajuste os filtros.'){return `<tr><td colspan="${cols}" class="empty">${icon('search')}<strong>${text}</strong>${hint}</td></tr>`;}
function orderRows(list,dashboard=false){return list.map(o=>`<tr><td><span class="os-id">#${String(o.id).padStart(4,'0')}</span></td><td><strong class="plate">${esc(o.placa)}</strong><small>${esc(o.marca)} ${esc(o.modelo)}</small></td><td>${esc(o.cliente)}${!dashboard?`<small>${esc(o.responsavel)}</small>`:''}</td><td>${badge(o.status)}</td>${!dashboard?`<td>${brl(o.total)}</td><td>${dt(o.aberta_em)}</td>`:''}<td><button class="table-action" data-order="${o.id}">Abrir</button></td></tr>`).join('')||empty(dashboard?5:7,'Nenhuma ordem neste filtro.');}
async function render(version=navigationVersion){
 const title=pages.find(([k])=>k===route)?.[1]||'Detalhes da ordem';$('#breadcrumb').textContent=title;
 document.title=(route==='ordem'?'Ordem #'+orderId:title)+' · Torq Oficina';
 if(route==='dashboard')renderDashboard();else if(route==='ordem')await renderOrder(version);else if(route==='ordens')renderOrders();else renderList();
}
function renderDashboard(){
 const d=data.dashboard,r=d.resumo,label=new Date().getHours()<12?'Bom dia':new Date().getHours()<18?'Boa tarde':'Boa noite';
 const metrics=[['Atendimentos ativos',r.atendimentos,'Veículos em atendimento','veiculos','ordens','ativas'],['Em execução',r.em_execucao,'Reparos em andamento','servicos','ordens','em_execucao'],['Prontos para entrega',r.prontas,'Ordens concluídas','check','ordens','concluida'],['Alertas de estoque',r.alertas,'Itens no mínimo ou abaixo','pecas','pecas','']];
 const stages=['aberta','em_execucao','aguardando_pecas','concluida'];
 const quick=allowed(['administrador','atendente'])?`<button class="secondary" data-new="clientes">${icon('clientes')} Novo cliente</button><button class="secondary" data-new="veiculos">${icon('veiculos')} Novo veículo</button>`:`<button class="secondary" data-nav="ordens">${icon('ordens')} Consultar ordens</button>`;
 $('#content').innerHTML=`<section class="dashboard-hero"><div class="hero-copy"><span class="eyebrow">SUA OFICINA, EM MOVIMENTO</span><h1>${label}, ${esc(user.nome.split(' ')[0])}.</h1><p>Uma visão clara de cada atendimento.<br>Seu próximo passo começa aqui.</p></div><div class="hero-right"><svg class="hero-car" viewBox="0 0 160 100" aria-hidden="true">${icons.car}</svg><div><span class="hero-label"><span class="online-dot"></span> OFICINA ROTA 4</span>${allowed(['administrador','atendente'])?newButton('ordens','Nova ordem de serviço'):`<button class="primary" data-nav="ordens">Acompanhar atendimentos ${icon('arrow')}</button>`}</div></div></section>`+
 `<div class="metrics">${metrics.map(([n,v,sub,i,page,status])=>`<button class="metric" data-nav="${page}" data-filter="${status}" ${page==='pecas'?'data-alerts="true"':''}><span class="metric-label">${n}</span><span class="metric-icon">${icon(i)}</span><strong>${v}</strong><small>${sub}</small></button>`).join('')}</div>`+
 `<div class="dashboard-grid"><div class="stack"><section class="panel"><div class="panel-head"><div><h2>Veículos em atendimento</h2><p>Do diagnóstico à entrega das chaves</p></div><button data-nav="ordens" data-filter="ativas">Ver todas ${icon('arrow')}</button></div><div class="table-wrap"><table><caption class="sr-only">Ordens ativas da oficina</caption><thead><tr><th scope="col">Ordem</th><th scope="col">Veículo</th><th scope="col">Cliente</th><th scope="col">Situação</th><th scope="col"><span class="sr-only">Ações</span></th></tr></thead><tbody>${orderRows(d.ordens.slice(0,6),true)}</tbody></table></div><div class="panel-note">${d.ordens.length>6?'Exibindo os 6 atendimentos mais recentes. ':''}Ordens entregues e canceladas permanecem disponíveis para consulta.</div></section><section class="panel"><div class="panel-head"><div><h2>Ritmo da operação</h2><p>Distribuição dos atendimentos ativos</p></div>${icon('ordens')}</div><div class="operation-flow">${stages.map(k=>`<button class="flow-stage" data-nav="ordens" data-filter="${k}"><span>${statuses[k]}</span><strong>${d.ordens.filter(o=>o.status===k).length}</strong><i></i></button>`).join('')}</div><div class="quick-actions">${quick}${allowed(['administrador'])?`<button class="secondary" data-movement>${icon('movimentacoes')} Repor estoque</button>`:''}</div></section></div>`+
 `<div class="stack"><section class="panel"><div class="panel-head"><div><h2>Atenção ao estoque</h2><p>Reposição no momento certo</p></div>${icon('alert')}</div>${d.alertas.slice(0,4).map(p=>`<div class="alert-item"><div class="alert-symbol">${icon('pecas')}</div><div><strong>${esc(p.nome)}</strong><p>${esc(p.codigo)} · Mínimo ${p.estoque_minimo} un.</p><small>${p.estoque} unidades disponíveis</small></div></div>`).join('')||'<div class="panel-note">Tudo em dia. Nenhuma peça atingiu o estoque mínimo.</div>'}<div class="panel-note"><button class="table-action" data-nav="pecas" data-alerts="true">Conferir reposição ${icon('arrow')}</button></div></section><section class="panel"><div class="panel-head"><div><h2>Últimas atividades</h2><p>Registros com data e responsável</p></div><button data-nav="historico">Ver histórico</button></div><div class="timeline">${data.historico.slice(0,4).map(h=>`<div class="timeline-item">${esc(eventText(h.evento))}<small>OS #${h.ordem_id} · ${dt(h.criado_em)}<br>${esc(h.usuario)}</small></div>`).join('')||'<div class="muted">Nenhuma atividade registrada.</div>'}</div></section></div></div>`+
 `<div class="flow-banner"><div><h3>Organização em cada detalhe.</h3><p>Clientes, veículos, serviços e estoque conectados. O histórico acompanha cada decisão.</p></div>${icon('servicos')}</div>`;
}
const sortOptions={
 ordens:[['id:desc','Mais recentes'],['id:asc','Mais antigas'],['total:desc','Maior valor'],['total:asc','Menor valor'],['cliente:asc','Cliente A–Z']],
 clientes:[['nome:asc','Nome A–Z'],['nome:desc','Nome Z–A'],['id:desc','Mais recentes']],
 veiculos:[['id:desc','Mais recentes'],['placa:asc','Placa A–Z'],['cliente:asc','Cliente A–Z']],
 servicos:[['nome:asc','Nome A–Z'],['preco:asc','Menor preço'],['preco:desc','Maior preço']],
 pecas:[['nome:asc','Nome A–Z'],['estoque:asc','Menor estoque'],['estoque:desc','Maior estoque'],['preco:asc','Menor preço']],
 usuarios:[['nome:asc','Nome A–Z'],['perfil:asc','Perfil']],
 movimentacoes:[['id:desc','Mais recentes'],['id:asc','Mais antigas'],['peca:asc','Peça A–Z']],
 historico:[['id:desc','Mais recentes'],['id:asc','Mais antigas']],
 auditoria:[['id:desc','Mais recentes'],['id:asc','Mais antigas'],['usuario:asc','Usuário A–Z']]
};
function toolbar(placeholder,extra=''){
 const choices=sortOptions[route]||[];if(!choices.some(([k])=>k===sortKey))sortKey=choices[0]?.[0]||'id:desc';
 return `<div class="toolbar"><label class="search" aria-label="Buscar registros">${icon('search')}<input id="search" type="search" aria-label="Buscar nesta lista" placeholder="${placeholder}" value="${esc(searchTerm)}" autocomplete="off"><button class="icon-button" id="clear-search" aria-label="Limpar busca" ${!searchTerm?'hidden':''}>✕</button></label><select id="sort-by" aria-label="Ordenar registros">${choices.map(([k,label])=>`<option value="${k}" ${sortKey===k?'selected':''}>${label}</option>`).join('')}</select>${extra}<button class="secondary" data-export>${icon('download')} Exportar CSV</button><span class="records" id="record-count" aria-live="polite"></span></div>`;
}
function dateFilters(){return `<div class="date-filters"><label for="date-from">De <input type="date" id="date-from" value="${esc(dateFrom)}"></label><label for="date-to">Até <input type="date" id="date-to" value="${esc(dateTo)}"></label><button id="clear-dates">Limpar período</button><span class="records">${route==='ordens'?'Data de abertura':'Data do registro'}</span></div>`;}
function pager(){return '<div class="pagination" id="pagination"></div>';}
function renderOrders(){
 const counts=[['','Todas',data.ordens.length],['ativas','Em atendimento',data.dashboard.resumo.atendimentos],...Object.entries(statuses).map(([k,n])=>[k,n,data.ordens.filter(o=>o.status===k).length])];
 $('#content').innerHTML=heading('Ordens de serviço','Do primeiro diagnóstico à entrega. Cada etapa, organizada.',allowed(['administrador','atendente'])?newButton('ordens','Nova ordem'):'')+toolbar('Buscar por ordem, placa ou cliente')+`<div class="status-counts">${counts.map(([k,n,v])=>`<button data-status="${k}" class="${filterStatus===k?'active':''}" aria-pressed="${filterStatus===k}">${n}<b>${v}</b></button>`).join('')}</div>`+dateFilters()+`<section class="panel"><div class="table-wrap"><table><caption class="sr-only">Ordens de serviço</caption><thead><tr><th scope="col">Ordem</th><th scope="col">Veículo</th><th scope="col">Cliente / responsável</th><th scope="col">Situação</th><th scope="col">Valor registrado</th><th scope="col">Abertura</th><th scope="col"><span class="sr-only">Ações</span></th></tr></thead><tbody id="rows"></tbody></table></div>${pager()}</section>`;filterRows();
}
const listConfig={
 clientes:{title:'Clientes',sub:'O primeiro passo de um bom atendimento.',create:['administrador','atendente'],button:'Novo cliente',cols:['Cliente','Documento','Telefone','E-mail',''],row:x=>`<td><strong>${esc(x.nome)}</strong><small>#${x.id}</small></td><td>${esc(x.documento)}</td><td>${esc(x.telefone)}</td><td>${esc(x.email)||'—'}</td>`},
 veiculos:{title:'Veículos',sub:'Cada veículo vinculado ao seu cliente e ao histórico da oficina.',create:['administrador','atendente'],button:'Novo veículo',cols:['Placa','Veículo','Cliente','Ano','Quilometragem',''],row:x=>`<td><strong class="plate">${esc(x.placa)}</strong></td><td>${esc(x.marca)} ${esc(x.modelo)}</td><td>${esc(x.cliente)}</td><td>${x.ano}</td><td>${Number(x.km).toLocaleString('pt-BR')} km</td>`},
 servicos:{title:'Serviços',sub:'Seu catálogo de mão de obra, com valores de referência.',create:['administrador'],button:'Novo serviço',cols:['Serviço','Descrição','Preço',''],row:x=>`<td><strong>${esc(x.nome)}</strong></td><td>${esc(x.descricao)}</td><td>${brl(x.preco)}</td>`},
 pecas:{title:'Peças e estoque',sub:'Consulte a disponibilidade e mantenha a reposição em dia.',create:['administrador'],button:'Nova peça',cols:['Código','Peça','Preço','Disponível','Mínimo','Situação',''],row:x=>`<td>${esc(x.codigo)}</td><td><strong>${esc(x.nome)}</strong></td><td>${brl(x.preco)}</td><td>${x.estoque} un.</td><td>${x.estoque_minimo} un.</td><td>${x.alerta?'<span class="badge aguardando_pecas">Repor estoque</span>':'<span class="badge concluida">Regular</span>'}</td>`},
 usuarios:{title:'Usuários',sub:'Controle os acessos da equipe e as permissões de cada perfil.',create:['administrador'],button:'Novo usuário',cols:['Nome','E-mail','Perfil','Situação',''],row:x=>`<td><strong>${esc(x.nome)}</strong></td><td>${esc(x.email)}</td><td>${roles[x.perfil]}</td><td>${x.ativo?'<span class="badge concluida">Ativo</span>':'<span class="badge cancelada">Inativo</span>'}</td>`},
 movimentacoes:{title:'Movimentações',sub:'Rastreie entradas, saídas e devoluções de peças. Últimos 500 registros.',create:[],cols:['Quando','Peça','Tipo','Quantidade','Saldo após','Ordem','Usuário','Motivo'],row:x=>`<td>${dt(x.criado_em)}</td><td><strong>${esc(x.peca)}</strong><small>${esc(x.codigo)}</small></td><td><span class="badge ${x.tipo==='saida'?'aguardando_pecas':'concluida'}">${{entrada:'Entrada',saida:'Saída',devolucao:'Devolução'}[x.tipo]}</span></td><td>${x.quantidade}</td><td>${x.saldo_apos}</td><td>${x.ordem_id?`<button class="table-action" data-order="${x.ordem_id}">#${x.ordem_id}</button>`:'—'}</td><td>${esc(x.usuario)}</td><td>${esc(x.motivo)}</td>`},
 historico:{title:'Histórico',sub:'Eventos das ordens com data, responsável e veículo. Últimos 500 registros.',create:[],cols:['Quando','Ordem','Veículo','Evento','Usuário'],row:x=>`<td>${dt(x.criado_em)}</td><td><button class="table-action" data-order="${x.ordem_id}">#${x.ordem_id}</button></td><td>${esc(x.placa)}</td><td>${esc(eventText(x.evento))}</td><td>${esc(x.usuario)}</td>`}
};
listConfig.auditoria={title:'Auditoria',sub:'Acessos e alterações registrados pelo servidor. Últimos 500 registros.',create:[],cols:['Quando','Usuário','Ação'],row:x=>`<td>${dt(x.criado_em)}</td><td>${esc(x.usuario)}</td><td class="wrap-cell">${esc(auditText(x.acao))}</td>`};
function renderList(){
 const c=listConfig[route];if(!c)return;
 let extra='';if(route==='pecas')extra=`<button class="toggle-filter" data-alert-filter aria-pressed="${onlyAlerts}">Somente reposição</button>`+(allowed(['administrador'])?`<button class="secondary" data-movement>${icon('movimentacoes')} Movimentar estoque</button>`:'');
 $('#content').innerHTML=heading(c.title,c.sub,allowed(c.create)?newButton(route,c.button):'')+toolbar('Buscar nesta lista',extra)+(['historico','movimentacoes','auditoria'].includes(route)?dateFilters():'')+(route==='pecas'&&data.dashboard.resumo.alertas?`<p class="inline-note">${data.dashboard.resumo.alertas} peça(s) no mínimo ou abaixo. Todas as mudanças de saldo ficam registradas em Movimentações.</p>`:'')+`<section class="panel"><div class="table-wrap"><table><caption class="sr-only">${c.title}</caption><thead><tr>${c.cols.map(v=>`<th scope="col">${v||'<span class="sr-only">Ações</span>'}</th>`).join('')}</tr></thead><tbody id="rows"></tbody></table></div>${pager()}</section>`;filterRows();
}
function filteredRows(){
 const list=data[route]||[],term=normalize(searchTerm);
 const selected=list.filter(x=>{
  const date=String(x.aberta_em||x.criado_em||'').slice(0,10);
  const statusOk=!filterStatus||filterStatus==='ativas'?!filterStatus||!['entregue','cancelada'].includes(x.status):x.status===filterStatus;
  return normalize([Object.values(x).join(' '),statuses[x.status]||'',roles[x.perfil]||'',eventText(x.evento),auditText(x.acao)].join(' ')).includes(term)&&statusOk&&(!onlyAlerts||route!=='pecas'||x.alerta)&&(!dateFrom||date>=dateFrom)&&(!dateTo||date<=dateTo);
 });
 const [key,direction]=sortKey.split(':'),factor=direction==='desc'?-1:1;
 return selected.sort((a,b)=>{
  let compare;if(['id','total','preco','estoque'].includes(key))compare=Number(a[key])-Number(b[key]);else compare=String(a[key]??'').localeCompare(String(b[key]??''),'pt-BR',{numeric:true,sensitivity:'base'});
  return factor*compare||Number(b.id)-Number(a.id);
 });
}
function filterRows(){
 if(!$('#rows'))return;
 const filtered=filteredRows(),total=filtered.length,pagesCount=Math.max(1,Math.ceil(total/pageSize));currentPage=Math.min(Math.max(1,currentPage),pagesCount);
 const from=(currentPage-1)*pageSize,list=filtered.slice(from,from+pageSize);
 if(route==='ordens')$('#rows').innerHTML=orderRows(list);else{const c=listConfig[route];$('#rows').innerHTML=list.map(x=>`<tr>${c.row(x)}${c.create.length?`<td>${allowed(c.create)?`<button class="table-action" data-edit="${x.id}">Editar</button>`:'—'}</td>`:''}</tr>`).join('')||empty(c.cols.length);}
 $('#record-count').textContent=`${total} registro${total===1?'':'s'}`;$('#clear-search').hidden=!searchTerm;
 $('#pagination').innerHTML=`<div class="pagination-label"><span>${total?from+1:0}–${Math.min(from+pageSize,total)} de ${total}</span><span>Por página</span><select id="page-size" aria-label="Registros por página">${[12,24,48].map(n=>`<option ${n===pageSize?'selected':''}>${n}</option>`).join('')}</select></div><div class="pagination-controls"><button data-page="${currentPage-1}" aria-label="Página anterior" ${currentPage===1?'disabled':''}>${icon('back')}</button><span>Página ${currentPage} de ${pagesCount}</span><button data-page="${currentPage+1}" aria-label="Próxima página" ${currentPage===pagesCount?'disabled':''}>${icon('arrow')}</button></div>`;savePreferences();
}
const exportFields={
 ordens:[['id','Ordem'],['placa','Placa'],['cliente','Cliente'],['responsavel','Responsável'],['status','Situação'],['total','Valor registrado'],['aberta_em','Abertura']],
 clientes:[['nome','Nome'],['documento','Documento'],['telefone','Telefone'],['email','E-mail']],
 veiculos:[['placa','Placa'],['marca','Marca'],['modelo','Modelo'],['cliente','Cliente'],['ano','Ano'],['km','Quilometragem']],
 servicos:[['nome','Serviço'],['descricao','Descrição'],['preco','Preço']],
 pecas:[['codigo','Código'],['nome','Peça'],['preco','Preço'],['estoque','Estoque'],['estoque_minimo','Mínimo']],
 usuarios:[['nome','Nome'],['email','E-mail'],['perfil','Perfil'],['ativo','Ativo']],
 movimentacoes:[['criado_em','Quando'],['peca','Peça'],['codigo','Código'],['tipo','Tipo'],['quantidade','Quantidade'],['saldo_apos','Saldo após'],['ordem_id','Ordem'],['usuario','Usuário'],['motivo','Motivo']],
 historico:[['criado_em','Quando'],['ordem_id','Ordem'],['placa','Placa'],['evento','Evento'],['usuario','Usuário']],
 auditoria:[['criado_em','Quando'],['usuario','Usuário'],['acao','Ação']]
};
function exportCsv(){
 const fields=exportFields[route],list=filteredRows();if(!list.length){toast('Não há registros neste filtro para exportar.',true);return;}
 const quote=v=>{let text=String(v??'');if(/^[\s]*[=+\-@]/.test(text))text="'"+text;return '"'+text.replace(/"/g,'""')+'"';};
 const csv='\uFEFF'+[fields.map(([,label])=>quote(label)).join(';'),...list.map(x=>fields.map(([key])=>quote(key==='status'?statuses[x[key]]:x[key])).join(';'))].join('\r\n');
 const url=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'})),link=document.createElement('a');link.href=url;link.download=`Torq_${route}_${new Date().toLocaleDateString('sv-SE')}.csv`;document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);toast(`${list.length} registro(s) exportados com os filtros atuais.`);
}
async function renderOrder(version=navigationVersion){
 const requestedId=orderId,d=await api('ordem?id='+requestedId);if(version!==navigationVersion||route!=='ordem'||orderId!==requestedId||!user)return;
 const o=d.ordem;currentOrder=o;const closed=['concluida','entregue','cancelada'].includes(o.status),canItems=allowed(['administrador','mecanico'])&&!closed;
 const serviceTotal=d.servicos.reduce((a,v)=>a+Number(v.preco_unitario)*v.quantidade,0),partTotal=d.pecas.reduce((a,v)=>a+Number(v.preco_unitario)*v.quantidade,0);
 const next=transitions[o.status].filter(s=>s==='entregue'||s==='cancelada'?allowed(['administrador','atendente']):true);
 function items(list,type){return `<div class="table-wrap"><table><thead><tr><th>${type==='servico'?'Serviço':'Peça'}</th><th>Qtd.</th><th>Unitário</th><th>Subtotal</th><th></th></tr></thead><tbody>${list.map(x=>`<tr><td><strong>${esc(x.nome)}</strong></td><td>${x.quantidade}</td><td>${brl(x.preco_unitario)}</td><td>${brl(x.preco_unitario*x.quantidade)}</td><td>${canItems?`<button class="table-action" data-remove="${type}" data-item="${x.id}">Remover</button>`:''}</td></tr>`).join('')||empty(5,'Nenhum item registrado.',canItems?'Use o botão acima para incluir itens neste atendimento.':'Este atendimento não possui itens nesta seção.')}</tbody></table></div>`;}
 $('#content').innerHTML=`<button class="back" data-nav="ordens">${icon('back')} Voltar às ordens</button>`+heading(`Ordem #${String(o.id).padStart(4,'0')}`,`${esc(o.placa)} · ${esc(o.marca)} ${esc(o.modelo)} · ${esc(o.cliente)}`,`${!closed&&allowed(['administrador','atendente'])?'<button class="secondary" data-order-edit>Editar atendimento</button>':''}<button class="secondary" data-print>Imprimir ordem</button>`)+orderFlow(o.status)+`<div class="detail-grid"><div class="stack"><section class="panel"><div class="panel-head"><h2>Dados do atendimento</h2>${badge(o.status)}</div><div class="info-grid"><div><span>Cliente</span><strong>${esc(o.cliente)}</strong></div><div><span>Responsável</span><strong>${esc(o.responsavel)}</strong></div><div><span>Abertura</span><strong>${dt(o.aberta_em)}</strong></div></div><div class="relato">${esc(o.relato)}</div></section><section class="panel"><div class="panel-head"><h2>Serviços realizados</h2>${canItems?`<button data-add="servico">+ Adicionar serviço</button>`:''}</div>${items(d.servicos,'servico')}</section><section class="panel"><div class="panel-head"><h2>Peças utilizadas</h2>${canItems?`<button data-add="peca">+ Adicionar peça</button>`:''}</div>${items(d.pecas,'peca')}</section>${o.status==='cancelada'?'<p class="inline-note">Ordem cancelada. As peças foram devolvidas ao estoque. Os itens permanecem registrados para consulta.</p>':''}</div><div class="stack"><section class="panel"><div class="panel-head"><h2>Resumo da ordem</h2></div><div class="summary"><p><span>Serviços</span><b>${brl(serviceTotal)}</b></p><p><span>Peças</span><b>${brl(partTotal)}</b></p><p class="total"><span>Valor registrado</span><strong>${brl(o.total)}</strong></p>${next.length?`<label for="next-status">Próxima etapa</label><select id="next-status">${next.map(s=>`<option value="${s}">${statuses[s]}</option>`).join('')}</select><button class="primary" data-update-status>Atualizar situação</button>`:`<p class="detail-footer">${o.status==='cancelada'?'Atendimento cancelado.':'Fluxo finalizado.'}</p>`}<p class="detail-footer">Os valores usam os preços registrados no momento da inclusão.</p></div></section><section class="panel"><div class="panel-head"><h2>Histórico do atendimento</h2>${icon('historico')}</div><div class="timeline">${d.historico.map(h=>`<div class="timeline-item">${esc(eventText(h.evento))}<small>${dt(h.criado_em)} · ${esc(h.usuario)}</small></div>`).join('')}</div></section></div></div>`;
}
const options=(list,label,value='id')=>list.map(x=>[x[value],label(x)]);
function field(name,label,type='text',opts={}){return {name,label,type,...opts};}
function formFields(kind,record={}){
 const clients=options(data.clientes,x=>x.nome),vehicles=options(data.veiculos.filter(v=>!data.ordens.some(o=>o.veiculo_id===v.id&&!['entregue','cancelada'].includes(o.status))),v=>`${v.placa} · ${v.modelo} · ${v.cliente}`);
 return {
 clientes:[field('nome','Nome completo','text',{max:120,wide:true}),field('documento','Documento / identificação','text',{max:20}),field('telefone','Telefone','tel',{max:25}),field('email','E-mail','email',{required:false,max:150,wide:true})],
 veiculos:[field('cliente_id','Cliente','select',{options:clients,wide:true}),field('placa','Placa','text',{max:10,placeholder:'ABC1D23'}),field('ano','Ano','number',{min:1900,max:2100,value:new Date().getFullYear()}),field('marca','Marca','text',{max:50}),field('modelo','Modelo','text',{max:60}),field('km','Quilometragem','number',{min:0,max:2147483647,value:0,wide:true})],
 servicos:[field('nome','Nome do serviço','text',{max:100,wide:true}),field('descricao','Descrição','textarea',{max:255,wide:true}),field('preco','Preço (R$)','number',{min:0,max:99999999.99,step:'.01',wide:true})],
 pecas:[field('codigo','Código','text',{max:30}),field('nome','Nome da peça','text',{max:100}),field('preco','Preço (R$)','number',{min:0,max:99999999.99,step:'.01'}),field('estoque_minimo','Estoque mínimo','number',{min:0,max:1000000,value:0}),...(!record.id?[field('estoque','Saldo inicial','number',{min:0,max:1000000,value:0,wide:true})]:[])],
 usuarios:[field('nome','Nome','text',{max:100,wide:true}),field('email','E-mail de acesso','email',{max:150,wide:true}),field('perfil','Perfil','select',{options:Object.entries(roles)}),...(record.id?[field('ativo','Situação','select',{options:[['1','Ativo'],['0','Inativo']],value:record.ativo?1:0})]:[]),field('senha',record.id?'Nova senha (opcional)':'Senha','password',{minLength:8,max:128,required:!record.id,wide:true})],
 ordem_dados:[field('responsavel_id','Responsável pelo atendimento','select',{options:options(data.responsaveis,x=>`${x.nome} (${roles[x.perfil]})`),wide:true}),field('relato','Relato / problema apresentado','textarea',{max:1000,wide:true})],
 ordens:[field('veiculo_id','Veículo disponível','select',{options:vehicles,wide:true}),field('responsavel_id','Responsável pelo atendimento','select',{options:options(data.responsaveis,x=>`${x.nome} (${roles[x.perfil]})`),wide:true}),field('relato','Relato / problema apresentado','textarea',{max:1000,wide:true})],
 movimentar:[field('peca_id','Peça','select',{options:options(data.pecas,p=>`${p.nome} (${p.estoque} disponíveis)`),wide:true}),field('tipo','Tipo','select',{options:[['entrada','Entrada / reposição'],['saida','Saída / ajuste']]}),field('quantidade','Quantidade','number',{min:1,max:1000000,value:1}),field('motivo','Motivo da movimentação','textarea',{max:255,wide:true})],
 servico:[field('servico_id','Serviço','select',{options:options(data.servicos,s=>`${s.nome} · ${brl(s.preco)}`),wide:true}),field('quantidade','Quantidade','number',{min:1,max:10000,value:1,wide:true})],
 peca:[field('peca_id','Peça disponível','select',{options:options(data.pecas.filter(p=>p.estoque>0),p=>`${p.nome} · ${p.estoque} un. · ${brl(p.preco)}`),wide:true}),field('quantidade','Quantidade','number',{min:1,max:10000,value:1,wide:true})]
 }[kind];
}

function orderFlow(status){
 if(status==='cancelada')return '<p class="inline-note">Atendimento cancelado. Os registros permanecem disponíveis para consulta.</p>';
 const steps=['aberta','em_execucao','concluida','entregue'],index=status==='aguardando_pecas'?1:steps.indexOf(status);
 return `<div class="order-flow" aria-label="Etapas do atendimento">${steps.map((k,i)=>`<div class="step ${i===index?'current':i<index?'passed':''}" ${i===index?'aria-current="step"':''}><b>${i<index?'✓':i+1}</b><span>${i===1&&status==='aguardando_pecas'?'Aguardando peças':statuses[k]}</span></div>`).join('')}</div>`;
}
function confirmAction(title,description,action='Confirmar',danger=false){
 const dialog=$('#confirm-modal');if(dialog.open)return Promise.resolve(false);
 $('#confirm-title').textContent=title;$('#confirm-description').textContent=description;$('#confirm-yes').textContent=action;$('#confirm-yes').className=danger?'danger':'primary';$('.confirm-icon').innerHTML=icon(danger?'alert':'check');
 return new Promise(resolve=>{dialog.returnValue='';dialog.addEventListener('close',()=>resolve(dialog.returnValue==='yes'),{once:true});$('#confirm-yes').onclick=()=>dialog.close('yes');$('#confirm-no').onclick=()=>dialog.close('no');dialog.showModal();$('#confirm-no').focus();});
}
async function closeForm(){
 if(formSaving)return;
 if(formDirty&&!await confirmAction('Descartar alterações?','Os dados preenchidos neste formulário ainda não foram salvos.','Descartar',true))return;
 formDirty=false;$('#modal').close();
}
$('#modal').addEventListener('cancel',e=>{e.preventDefault();closeForm();});
window.addEventListener('beforeunload',e=>{if(formDirty){e.preventDefault();e.returnValue='';}});
function formPreview(){
 const box=$('#form-preview');if(!box||!formContext)return;
 const kind=formContext.kind,quantity=Number($('#f-quantidade')?.value||0);
 let selected;if(kind==='servico')selected=data.servicos.find(x=>String(x.id)===$('#f-servico_id').value);else selected=data.pecas.find(x=>String(x.id)===$('#f-peca_id').value);
 if(!selected){box.hidden=true;return;}box.hidden=false;
 const isOut=kind==='peca'||(kind==='movimentar'&&$('#f-tipo').value==='saida'),stock=Number(selected.estoque||0),after=stock+(isOut?-quantity:quantity),insufficient=isOut&&quantity>stock;
 box.classList.toggle('warning',insufficient);
 box.innerHTML=kind==='movimentar'?`<span>Saldo previsto após a movimentação</span><strong>${after} unidades</strong><p>Disponível agora: ${stock} un. ${insufficient?'A quantidade supera o estoque disponível.':'O motivo e o responsável ficarão registrados.'}</p>`:`<span>Subtotal previsto</span><strong>${brl(Number(selected.preco)*quantity)}</strong><p>${kind==='peca'?`Disponível: ${stock} un. · Saldo previsto: ${after} un. ${insufficient?'Quantidade acima do estoque.':''}`:'Preço do catálogo: '+brl(selected.preco)+' por unidade.'}<br>O servidor confirma o preço e o saldo ao salvar.</p>`;
}
function openForm(kind,record={}){
 const titles={clientes:'cliente',veiculos:'veículo',servicos:'serviço',pecas:'peça',usuarios:'usuário',ordens:'ordem de serviço',ordem_dados:'atendimento',servico:'serviço à ordem',peca:'peça à ordem',movimentar:'movimentação de estoque'};
 const fields=formFields(kind,record),openedOrder=orderId;formContext={kind,record};formDirty=false;formSaving=false;
 $('#modal-title').textContent=`${record.id?'Editar':'Registrar'} ${titles[kind]}`;
 const hints={documento:'Identificação única do cliente.',placa:'Aceita ABC1D23 ou ABC1234. O formato será padronizado.',senha:record.id?'Deixe vazio para manter a senha atual.':'Use pelo menos 8 caracteres.',relato:'Descreva os sintomas e o que precisa ser verificado.',estoque_minimo:'Alerta quando o saldo for igual ou menor que este valor.'};
 $('#modal-body').innerHTML=`<form id="editor-form"><div class="form-grid">${fields.map(f=>{
  let v=record[f.name]??f.value??'';if(f.name==='senha')v='';
  const attrs=`id="f-${f.name}" name="${f.name}" ${f.required===false?'':'required'} ${f.min!==undefined?`min="${f.min}"`:''} ${f.max!==undefined&&f.type==='number'?`max="${f.max}"`:f.max!==undefined?`maxlength="${f.max}"`:''} ${f.step?`step="${f.step}"`:''} ${f.minLength?`minlength="${f.minLength}"`:''} ${hints[f.name]?`aria-describedby="hint-${f.name}"`:''}`;
  let input=f.type==='select'?`<select ${attrs}><option value="">${f.options.length?'Selecione…':'Nenhum registro disponível'}</option>${f.options.map(([value,label])=>`<option value="${esc(value)}" ${String(value)===String(v)?'selected':''}>${esc(label)}</option>`).join('')}</select>`:f.type==='textarea'?`<textarea ${attrs}>${esc(v)}</textarea>`:`<input ${attrs} type="${f.type}" value="${esc(v)}" placeholder="${esc(f.placeholder||'')}" ${f.type==='password'?'autocomplete="new-password"':''}>`;
  return `<div class="${f.wide?'span-2':''}"><label for="f-${f.name}">${f.label}${f.required===false?' <span class="muted">· opcional</span>':''}</label>${input}${hints[f.name]?`<small class="field-hint" id="hint-${f.name}">${hints[f.name]}</small>`:''}</div>`;
 }).join('')}</div>${['peca','servico','movimentar'].includes(kind)?'<div id="form-preview" class="preview-card" aria-live="polite" hidden></div>':''}${kind==='pecas'&&record.id?'<p class="inline-note">Para alterar o saldo, use “Movimentar estoque”.</p>':''}${fields.some(f=>f.type==='select'&&!f.options.length)?'<p class="inline-note">Não há registros disponíveis para uma das seleções. Confira os cadastros e o estoque antes de continuar.</p>':''}<p class="error" id="form-error" role="alert"></p><div class="form-actions"><button class="secondary" type="button" data-close>Cancelar</button><button class="primary" type="submit">Salvar ${icon('check')}</button></div></form>`;
 const form=$('#editor-form');form.addEventListener('input',()=>{formDirty=true;formPreview();});form.addEventListener('change',()=>{formDirty=true;formPreview();});
 form.addEventListener('submit',async e=>{
  e.preventDefault();if(formSaving)return;
  const button=e.target.querySelector('[type="submit"]'),error=$('#form-error'),payload=Object.fromEntries(new FormData(e.target));error.textContent='';
  if(record.id)payload.id=record.id;if(['servico','peca','ordem_dados'].includes(kind))payload.ordem_id=openedOrder;
  if(kind==='veiculos')payload.placa=payload.placa.toUpperCase().replace(/[\s-]/g,'');
  if(kind==='peca'||(kind==='movimentar'&&payload.tipo==='saida')){
   const part=data.pecas.find(p=>String(p.id)===payload.peca_id);if(part&&Number(payload.quantidade)>Number(part.estoque)){error.textContent=`Estoque insuficiente. Disponível: ${part.estoque}.`;return;}
  }
  formSaving=true;button.disabled=true;button.textContent='Salvando…';$('#close-modal').disabled=true;e.target.querySelector('[data-close]').disabled=true;
  try{
   const path=['servico','peca'].includes(kind)?'ordem/'+kind:kind==='ordem_dados'?'ordem/dados':kind==='movimentar'?'estoque/movimentar':kind;
   const saved=await api(path,payload);formDirty=false;$('#modal').close();
   try{await refresh();if(kind==='ordens')await navigate('ordem',saved.id);else{startUI();await render();}toast('Registro salvo com sucesso.');}
   catch(err){toast('Registro salvo. A atualização da tela falhou: '+err.message,true);}
  }catch(err){if($('#modal').open)error.textContent=err.message;else toast(err.message,true);}
  finally{formSaving=false;button.disabled=false;button.innerHTML='Salvar '+icon('check');$('#close-modal').disabled=false;if(e.target.querySelector('[data-close]'))e.target.querySelector('[data-close]').disabled=false;}
 });
 $('#modal').showModal();form.querySelector('input,select,textarea')?.focus();formPreview();
}

function openSearch(){if(!user)return;$('#global-query').value='';commandIndex=0;searchCommands();$('#search-modal').showModal();$('#global-query').focus();}
function searchCommands(){
 const term=normalize($('#global-query').value),matches=v=>normalize(v).includes(term),items=[];
 for(const [page,label] of visiblePages())if(matches(label))items.push({group:'Páginas',page,label,sub:'Abrir '+label,icon:page});
 if(term){
  for(const o of data.ordens.filter(o=>matches('#'+o.id+' '+o.placa+' '+o.cliente)).slice(0,6))items.push({group:'Ordens',page:'ordem',id:o.id,label:`OS #${o.id} · ${o.placa}`,sub:o.cliente+' · '+statuses[o.status],icon:'ordens'});
  for(const c of data.clientes.filter(c=>matches(c.nome+' '+c.documento+' '+c.telefone)).slice(0,5))items.push({group:'Clientes',page:'clientes',search:c.documento,label:c.nome,sub:c.documento+' · '+c.telefone,icon:'clientes'});
  for(const v of data.veiculos.filter(v=>matches(v.placa+' '+v.marca+' '+v.modelo)).slice(0,5))items.push({group:'Veículos',page:'veiculos',search:v.placa,label:v.placa+' · '+v.marca+' '+v.modelo,sub:v.cliente,icon:'veiculos'});
 }
 commandItems=items;commandIndex=0;let group='';$('#search-results').innerHTML=items.map((item,i)=>{const section=item.group!==group?`<div class="command-section">${item.group}</div>`:'';group=item.group;return section+`<button class="command-result ${i===0?'selected':''}" data-command="${i}">${icon(item.icon)}<span>${esc(item.label)}<small>${esc(item.sub)}</small></span><span>↵</span></button>`;}).join('')||'<div class="empty"><strong>Nenhum resultado</strong>Tente um nome, uma placa ou o número da ordem.</div>';
}
async function activateCommand(index){const item=commandItems[index];if(!item)return;$('#search-modal').close();await navigate(item.page,item.id,{search:item.search??'',status:'',alerts:false});}
document.addEventListener('keydown',e=>{
 if($('#search-modal').open){
  if(e.key==='ArrowDown'||e.key==='ArrowUp'){e.preventDefault();commandIndex=Math.max(0,Math.min(commandItems.length-1,commandIndex+(e.key==='ArrowDown'?1:-1)));document.querySelectorAll('[data-command]').forEach((b,i)=>b.classList.toggle('selected',i===commandIndex));$(`[data-command="${commandIndex}"]`)?.scrollIntoView({block:'nearest'});}
  if(e.key==='Enter'){e.preventDefault();activateCommand(commandIndex).catch(err=>toast(err.message,true));}return;
 }
 const editing=e.target.matches('input,select,textarea,[contenteditable="true"]');
 if(user&&!$('#modal').open&&!$('#confirm-modal').open&&((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'||e.key==='/'&&!editing)){e.preventDefault();openSearch();}
});
document.addEventListener('input',e=>{
 if(e.target.id==='search'){searchTerm=e.target.value;currentPage=1;filterRows();}
 if(e.target.id==='global-query')searchCommands();
});
document.addEventListener('change',e=>{
 if(e.target.id==='sort-by'){sortKey=e.target.value;currentPage=1;filterRows();}
 if(e.target.id==='page-size'){pageSize=Number(e.target.value);currentPage=1;filterRows();}
 if(e.target.id==='date-from'||e.target.id==='date-to'){dateFrom=$('#date-from').value;dateTo=$('#date-to').value;if(dateFrom&&dateTo&&dateFrom>dateTo){toast('A data inicial precisa ser anterior ou igual à data final.',true);e.target.value='';if(e.target.id==='date-from')dateFrom='';else dateTo='';}currentPage=1;filterRows();}
});
document.addEventListener('click',async e=>{
 const button=e.target.closest('button');if(!button||button.disabled)return;
 try{
  if(button.dataset.nav)await navigate(button.dataset.nav,null,{...(button.hasAttribute('data-filter')?{status:button.dataset.filter,search:''}:{}),...(button.hasAttribute('data-alerts')?{alerts:true,search:''}:{})});
  else if(button.dataset.order)await navigate('ordem',button.dataset.order);
  else if(button.dataset.new)openForm(button.dataset.new);
  else if(button.dataset.edit)openForm(route,data[route].find(x=>String(x.id)===button.dataset.edit));
  else if(button.hasAttribute('data-movement'))openForm('movimentar');
  else if(button.dataset.add)openForm(button.dataset.add);
  else if(button.hasAttribute('data-order-edit'))openForm('ordem_dados',currentOrder);
  else if(button.hasAttribute('data-close')||button.id==='close-modal')await closeForm();
  else if(button.hasAttribute('data-status')){filterStatus=button.dataset.status;currentPage=1;document.querySelectorAll('[data-status]').forEach(b=>{b.classList.toggle('active',b===button);b.setAttribute('aria-pressed',b===button);});filterRows();}
  else if(button.hasAttribute('data-alert-filter')){onlyAlerts=!onlyAlerts;currentPage=1;button.setAttribute('aria-pressed',onlyAlerts);filterRows();}
  else if(button.hasAttribute('data-page')){currentPage=Number(button.dataset.page);filterRows();}
  else if(button.hasAttribute('data-export'))exportCsv();
  else if(button.hasAttribute('data-command'))await activateCommand(Number(button.dataset.command));
  else if(button.id==='clear-search'){searchTerm='';$('#search').value='';currentPage=1;filterRows();$('#search').focus();}
  else if(button.id==='clear-dates'){dateFrom=dateTo='';$('#date-from').value=$('#date-to').value='';currentPage=1;filterRows();}
  else if(button.id==='global-search')openSearch();
  else if(button.id==='close-search')$('#search-modal').close();
  else if(button.hasAttribute('data-update-status')){
   const status=$('#next-status').value,openedOrder=orderId;button.disabled=true;
   if(['cancelada','entregue'].includes(status)&&!await confirmAction(status==='cancelada'?'Cancelar atendimento?':'Confirmar entrega?',status==='cancelada'?'As peças utilizadas serão devolvidas ao estoque. A ordem continuará disponível para consulta.':'Confirme que o veículo foi entregue ao cliente. A ordem será encerrada e ficará disponível para consulta.',status==='cancelada'?'Cancelar atendimento':'Confirmar entrega',status==='cancelada'))return;
   await api('ordem/status',{ordem_id:openedOrder,status});try{await refresh();startUI();await render();toast('Situação atualizada.');}catch(err){toast('Situação salva. '+err.message,true);}
  }
  else if(button.dataset.remove){button.disabled=true;const openedOrder=orderId;
   if(!await confirmAction('Remover item da ordem?',button.dataset.remove==='peca'?'A quantidade deste item voltará ao estoque. A devolução será registrada no histórico.':'O serviço será removido do valor da ordem e a operação ficará registrada.','Remover item',true))return;
   await api('ordem/remover-'+button.dataset.remove,{ordem_id:openedOrder,item_id:button.dataset.item});try{await refresh();await render();toast('Item removido.');}catch(err){toast('Item removido. '+err.message,true);}
  }
  else if(button.hasAttribute('data-print'))window.print();
  else if(button.id==='alert-shortcut')await navigate('pecas',null,{alerts:true,search:''});
  else if(button.id==='refresh-data'){button.disabled=true;await refresh();startUI();await render();toast('Dados atualizados.');}
  else if(button.id==='logout'){button.disabled=true;await api('logout',{});showLogin();$('#login-password').value='';$('#login-error').textContent='';try{Object.keys(sessionStorage).filter(k=>k.startsWith('torq:')).forEach(k=>sessionStorage.removeItem(k));}catch{}}
 }catch(err){toast(err.message,true);}
 finally{if(button.isConnected)button.disabled=button.id==='refresh-data'&&requests>0;}
});
$('#login-form').addEventListener('submit',async e=>{e.preventDefault();const btn=e.target.querySelector('button');if(btn.disabled)return;btn.disabled=true;btn.textContent='Entrando…';$('#login-error').textContent='';try{const result=await api('login',Object.fromEntries(new FormData(e.target)));user=result.usuario;csrf=result.csrf;await refresh();await routeFromHash();}catch(err){showLogin();$('#login-error').textContent=err.message;}finally{btn.disabled=false;btn.innerHTML='Entrar na oficina <span aria-hidden="true">↗</span>';}});
(async()=>{try{const result=await api('me');user=result.usuario;csrf=result.csrf;await refresh();await routeFromHash();}catch(err){showLogin();if(!err.message.includes('Entre para continuar'))$('#login-error').textContent=err.message;}})();
