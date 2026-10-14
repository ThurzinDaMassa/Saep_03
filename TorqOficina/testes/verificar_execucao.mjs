// Verifica a inicialização e a orientação quando o HTML é aberto diretamente.
// Opcional: --persistencia confirma os dados gravados pelos testes anteriores após reiniciar.
import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const { chromium } = await import(process.env.TORQ_PLAYWRIGHT_MODULE || 'playwright');
const args = process.argv.slice(2);
const option = (name, fallback) => args.includes(name) ? args[args.indexOf(name) + 1] : fallback;
const url = option('--url', 'http://127.0.0.1:8080');
const output = option('--saida', 'testes/resultado_inicializacao_organizado.json');
const cases = [];
const check = (condition, title) => {
    if (!condition) throw new Error(title);
    cases.push(title);
    console.log('APROVADO: ' + title);
};

const browser = await chromium.launch({ channel: 'msedge', headless: true });
try {
    const page = await browser.newPage();
    await page.goto(pathToFileURL(path.resolve('web/index.html')).href);
    await page.locator('#login-error').waitFor({ state: 'visible' });
    check((await page.locator('#login-error').textContent()).includes('INICIAR_SISTEMA.bat'),
        'HTML aberto diretamente explica como iniciar o sistema');
    check(await page.locator('#login-form button[type="submit"]').isDisabled(),
        'HTML sem servidor não oferece um login inoperante');

    await page.goto(url);
    await page.locator('#login-form').waitFor({ state: 'visible' });
    check((await page.request.get(url + '/api/me')).status() === 401,
        'Servidor está ativo e exige uma nova sessão');
    await page.locator('#login-email').fill('admin@torq.local');
    await page.locator('#login-password').fill('Oficina@123');
    await page.getByRole('button', { name: 'Entrar na oficina' }).click();
    await page.locator('.metrics').waitFor();
    const clients = await page.evaluate(async () => (await fetch('/api/clientes')).json());
    const orders = await page.evaluate(async () => (await fetch('/api/ordens')).json());
    check(clients.length >= 3 && orders.length >= 3,
        'Navegador autentica e consulta cadastros e ordens do banco');
    await page.evaluate(() => { window.scrollTo(0, 0); document.activeElement?.blur(); });
    await fs.mkdir('testes/evidencias_organizado', { recursive: true });
    await page.screenshot({ path: 'testes/evidencias_organizado/sistema_funcionando.png',
        fullPage: true, animations: 'disabled', style: '#toast-region{visibility:hidden}' });
    if (args.includes('--persistencia')) {
        check(clients.some(client => client.nome.startsWith('Cliente Alterado ')) &&
            orders.some(order => order.status === 'entregue'),
            'Cadastros e ordens gravados nos testes permanecem após reiniciar');
    }
    await fs.mkdir(path.dirname(output), { recursive: true });
    await fs.writeFile(output, JSON.stringify({
        data_execucao: new Date().toISOString(), url, total: cases.length,
        aprovados: cases.length, casos: cases
    }, null, 2));
} finally {
    await browser.close();
}
