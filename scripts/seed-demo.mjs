#!/usr/bin/env node
// Populates the running API with demo data: a regular user and the admin, each with orders in every status.
// Usage: node scripts/seed-demo.mjs [apiUrl] [--force]   (default http://localhost:8080/api/v1, Node 22+)
// The admin comes from backend/.env (the API creates it on startup); the regular user is created here.
// Runs once: if the regular user already has orders it stops, unless --force adds another batch.

import { readFileSync } from "node:fs";

const args = process.argv.slice(2);
if (args.includes("--help") || args.includes("-h")) {
  console.log(
    "Uso: node scripts/seed-demo.mjs [apiUrl] [--force]\n" +
      "  apiUrl   padrão http://localhost:8080/api/v1\n" +
      "  --force  cria mais um lote mesmo se o banco já estiver populado",
  );
  process.exit(0);
}
const API = (
  args.find((arg) => !arg.startsWith("--")) ?? "http://localhost:8080/api/v1"
).replace(/\/$/, "");
const FORCE = args.includes("--force");

function readBackendEnv() {
  try {
    const text = readFileSync(
      new URL("../backend/.env", import.meta.url),
      "utf8",
    );
    return Object.fromEntries(
      text
        .split(/\r?\n/)
        .map((line) => line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/))
        .filter(Boolean)
        .map(([, key, value]) => [key, value]),
    );
  } catch {
    return {};
  }
}

const backendEnv = readBackendEnv();
const ADMIN = {
  email:
    process.env.SEED_ADMIN_EMAIL ??
    backendEnv.APP_SECURITY_ADMIN_EMAIL ??
    "admin@foody.local",
  password:
    process.env.SEED_ADMIN_PASSWORD ??
    backendEnv.APP_SECURITY_ADMIN_PASSWORD ??
    "admin-dev-password",
};
const USER = {
  name: "Maria Oliveira",
  email: "usuario@foody.local",
  password: "usuario-dev-password",
};

class ApiError extends Error {
  constructor(status, data) {
    super(data?.detail ?? `HTTP ${status}`);
    this.status = status;
  }
}

async function call(method, path, { token, body } = {}) {
  let res;
  try {
    res = await fetch(API + path, {
      method,
      headers: {
        ...(body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    fail(
      `A API não respondeu em ${API}.`,
      "Suba o backend antes: cd backend && docker compose up --build -d",
    );
  }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

function fail(...lines) {
  console.error(`\nErro: ${lines.join("\n      ")}`);
  process.exit(1);
}

async function login({ email, password }) {
  const { accessToken } = await call("POST", "/auth/login", {
    body: { email, password },
  });
  return accessToken;
}

// --- Demo data ---------------------------------------------------------------

const CUSTOMERS = [
  ["José da Silva", "+5511987521144", "jose.silva@exemplo.com"],
  ["Mariana Costa", "+5511991234567", null],
  ["Carlos Pereira", "+5511976543210", "carlos.pereira@exemplo.com"],
  ["Ana Beatriz Lima", "+5511965432109", null],
  ["Rafael Souza", "+5511954321098", "rafael.souza@exemplo.com"],
  ["Fernanda Oliveira", null, "fernanda.oli@exemplo.com"],
  ["Lucas Almeida", "+5511932109876", null],
  ["Juliana Martins", "+5511921098765", "ju.martins@exemplo.com"],
  ["Pedro Henrique Rocha", "+5519998877665", null],
  ["Camila Ribeiro", "+5511910987654", "camila.ribeiro@exemplo.com"],
  ["Thiago Carvalho", null, null],
  ["Beatriz Gomes", "+5511998765432", "bia.gomes@exemplo.com"],
  ["Gabriel Fernandes", "+5521987654321", null],
  ["Larissa Barbosa", "+5511987654320", "larissa.b@exemplo.com"],
  ["Felipe Araújo", "+5511976543219", null],
  ["Patrícia Mendes", "+5511965432108", "patricia.mendes@exemplo.com"],
  ["Rodrigo Teixeira", "+5531998765432", null],
  ["Aline Cardoso", "+5511954321097", "aline.cardoso@exemplo.com"],
  ["Bruno Nascimento", "+5511943210986", null],
  ["Vanessa Duarte", null, "vanessa.duarte@exemplo.com"],
  ["Eduardo Moreira", "+5541998877665", null],
  ["Renata Castro", "+5511932109875", "renata.castro@exemplo.com"],
  ["Gustavo Pinto", "+5511921098764", null],
  ["Isabela Freitas", "+5511910987653", "isa.freitas@exemplo.com"],
  ["Marcelo Correia", "+5511998765431", null],
  ["Letícia Santos", "+5511987654319", "leticia.santos@exemplo.com"],
];

const ADDRESSES = [
  [
    "Rua José Neves",
    "590",
    "Sala 03",
    "Vila São Paulo",
    "São Paulo",
    "SP",
    "04650-140",
  ],
  [
    "Alameda dos Maracatins",
    "1217",
    "Apto 82",
    "Moema",
    "São Paulo",
    "SP",
    "04089-014",
  ],
  [
    "Rua dos Pinheiros",
    "870",
    null,
    "Pinheiros",
    "São Paulo",
    "SP",
    "05422-001",
  ],
  [
    "Rua Domingos de Morais",
    "2187",
    "Bloco B",
    "Vila Mariana",
    "São Paulo",
    "SP",
    "04035-000",
  ],
  ["Avenida Ibirapuera", "3103", null, "Moema", "São Paulo", "SP", "04029-902"],
  [
    "Rua Teodoro Sampaio",
    "1020",
    "Casa 2",
    "Pinheiros",
    "São Paulo",
    "SP",
    "05406-050",
  ],
  [
    "Rua Augusta",
    "1508",
    "Apto 41",
    "Consolação",
    "São Paulo",
    "SP",
    "01304-001",
  ],
  [
    "Rua Oscar Freire",
    "379",
    null,
    "Jardim Paulista",
    "São Paulo",
    "SP",
    "01426-001",
  ],
  [
    "Avenida Norte-Sul",
    "1200",
    "Apto 1503",
    "Cambuí",
    "Campinas",
    "SP",
    "13025-320",
  ],
  [
    "Rua Harmonia",
    "245",
    null,
    "Vila Madalena",
    "São Paulo",
    "SP",
    "05435-000",
  ],
  ["Rua Tuiuti", "1890", "Fundos", "Tatuapé", "São Paulo", "SP", "03307-005"],
  [
    "Avenida Santo Amaro",
    "4560",
    null,
    "Brooklin",
    "São Paulo",
    "SP",
    "04556-200",
  ],
  [
    "Rua Visconde de Pirajá",
    "414",
    "Apto 302",
    "Ipanema",
    "Rio de Janeiro",
    "RJ",
    "22410-002",
  ],
  [
    "Rua Cardeal Arcoverde",
    "2365",
    null,
    "Pinheiros",
    "São Paulo",
    "SP",
    "05408-003",
  ],
  [
    "Rua Vergueiro",
    "3185",
    "Apto 12",
    "Vila Mariana",
    "São Paulo",
    "SP",
    "04101-300",
  ],
  [
    "Rua Bela Cintra",
    "1149",
    null,
    "Consolação",
    "São Paulo",
    "SP",
    "01415-001",
  ],
  [
    "Avenida do Contorno",
    "6061",
    "Loja 2",
    "Savassi",
    "Belo Horizonte",
    "MG",
    "30110-035",
  ],
  [
    "Rua Mourato Coelho",
    "936",
    null,
    "Pinheiros",
    "São Paulo",
    "SP",
    "05417-001",
  ],
  ["Rua Itapura", "1200", "Apto 74", "Tatuapé", "São Paulo", "SP", "03310-000"],
  [
    "Rua Joaquim Floriano",
    "466",
    null,
    "Itaim Bibi",
    "São Paulo",
    "SP",
    "04534-002",
  ],
  [
    "Rua XV de Novembro",
    "700",
    "Sala 5",
    "Centro",
    "Curitiba",
    "PR",
    "80020-310",
  ],
  [
    "Rua Sena Madureira",
    "1355",
    null,
    "Vila Clementino",
    "São Paulo",
    "SP",
    "04021-051",
  ],
  ["Rua Clélia", "1520", "Casa", "Lapa", "São Paulo", "SP", "05042-000"],
  ["Avenida Moema", "170", "Apto 21", "Moema", "São Paulo", "SP", "04077-020"],
  [
    "Rua Fradique Coutinho",
    "1100",
    null,
    "Vila Madalena",
    "São Paulo",
    "SP",
    "05416-001",
  ],
  [
    "Rua Apeninos",
    "990",
    "Apto 133",
    "Paraíso",
    "São Paulo",
    "SP",
    "04104-020",
  ],
];

const item = (description, quantity, unitPrice) => ({
  description,
  quantity,
  unitPrice,
});

const MENUS = [
  [
    [item("Pizza calabresa G", 1, 59.9), item("Guaraná 2 L", 1, 14.95)],
    "Sem cebola.",
  ],
  [[item("Temaki salmão", 2, 32.5), item("Hot roll 10 un.", 1, 29.9)], null],
  [
    [item("Hambúrguer artesanal", 2, 38), item("Batata frita", 1, 18.5)],
    "Ponto da carne: mal passado.",
  ],
  [[item("Yakisoba de frango", 1, 44.9)], null],
  [
    [item("Pizza margherita G", 1, 54.9), item("Pizza de chocolate M", 1, 42)],
    "Troco para R$ 100.",
  ],
  [[item("Açaí 500 ml", 2, 24), item("Granola extra", 2, 3.5)], null],
  [
    [
      item("Marmita executiva de frango", 3, 27.9),
      item("Suco de laranja 500 ml", 3, 9.9),
    ],
    "Entregar na portaria.",
  ],
  [[item("Poke de salmão", 1, 49.9), item("Água com gás", 1, 6)], null],
  [[item("Combinado 30 peças", 1, 119.9)], "Sem wasabi, por favor."],
  [
    [
      item("Esfiha de carne", 6, 7.5),
      item("Esfiha de queijo", 4, 7.5),
      item("Kibe frito", 2, 8),
    ],
    null,
  ],
  [
    [item("Feijoada individual", 1, 46.9), item("Caipirinha de limão", 1, 22)],
    null,
  ],
  [[item("Salada caesar com frango", 1, 36.9)], "Molho à parte."],
  [
    [item("Parmegiana de carne", 2, 52.9), item("Refrigerante lata", 2, 7)],
    null,
  ],
  [
    [
      item("Tapioca de queijo coalho", 2, 19.9),
      item("Café coado 300 ml", 2, 8.5),
    ],
    null,
  ],
  [
    [item("Lasanha à bolonhesa", 1, 48.9), item("Pudim de leite", 1, 14.9)],
    "Tocar o interfone 21.",
  ],
  [
    [item("Burrito de carne", 2, 34.9), item("Nachos com guacamole", 1, 29.9)],
    null,
  ],
];

const FLOW = ["EM_PREPARO", "SAIU_PARA_ENTREGA", "ENTREGUE"];
const until = (status) => FLOW.slice(0, FLOW.indexOf(status) + 1);

// Oldest first, so the API lists the newest at the top. Status changes are made by the admin acting as
// the restaurant; cancellations by whoever placed the order.
const PLAN = [
  ["user", until("ENTREGUE")],
  ["admin", until("ENTREGUE")],
  ["user", until("ENTREGUE")],
  ["admin", until("ENTREGUE")],
  ["user", ["CANCELADO"]],
  ["admin", until("ENTREGUE")],
  ["user", until("ENTREGUE")],
  ["admin", [...until("EM_PREPARO"), "CANCELADO"]],
  ["user", until("ENTREGUE")],
  ["admin", until("ENTREGUE")],
  ["user", [...until("EM_PREPARO"), "CANCELADO"]],
  ["user", until("SAIU_PARA_ENTREGA")],
  ["admin", until("SAIU_PARA_ENTREGA")],
  ["user", until("SAIU_PARA_ENTREGA")],
  ["admin", until("SAIU_PARA_ENTREGA")],
  ["user", until("EM_PREPARO")],
  ["admin", until("EM_PREPARO")],
  ["user", until("EM_PREPARO")],
  ["admin", until("EM_PREPARO")],
  ["user", until("EM_PREPARO")],
  ["admin", []],
  ["user", []],
  ["admin", []],
  ["user", []],
  ["admin", []],
  ["user", []],
];

function buildOrder(index) {
  const [name, phone, email] = CUSTOMERS[index % CUSTOMERS.length];
  const [street, number, complement, neighborhood, city, state, postalCode] =
    ADDRESSES[index % ADDRESSES.length];
  const [items, notes] = MENUS[index % MENUS.length];
  return {
    customer: { name, ...(phone && { phone }), ...(email && { email }) },
    items,
    deliveryAddress: {
      street,
      number,
      ...(complement && { complement }),
      neighborhood,
      city,
      state,
      postalCode,
    },
    ...(notes && { notes }),
  };
}

// --- Run ---------------------------------------------------------------------

const created = { user: 0, admin: 0 };

async function main() {
  console.log(`Populando ${API}\n`);

  let adminToken;
  try {
    adminToken = await login(ADMIN);
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error;
    fail(
      `Não consegui entrar como admin (${ADMIN.email}).`,
      "Confira APP_SECURITY_ADMIN_EMAIL / APP_SECURITY_ADMIN_PASSWORD em backend/.env",
      "(o admin é criado quando a API sobe) ou passe SEED_ADMIN_EMAIL / SEED_ADMIN_PASSWORD.",
    );
  }
  console.log(`ok  admin ${ADMIN.email}`);

  try {
    await call("POST", "/auth/register", { body: USER });
    console.log(`ok  usuário ${USER.email} criado`);
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 409) throw error;
    console.log(`ok  usuário ${USER.email} já existe`);
  }

  let userToken;
  try {
    userToken = await login(USER);
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error;
    fail(
      `O usuário ${USER.email} existe, mas a senha não é mais "${USER.password}".`,
      'Redefina em http://localhost:5173/forgot-password ou recrie o banco com "docker compose down -v".',
    );
  }

  const existing = await call("GET", "/orders?size=1", { token: userToken });
  if (existing.totalElements > 0 && !FORCE) {
    console.log(
      `\nO banco já está populado (${existing.totalElements} pedidos de ${USER.email}). Nada a fazer.` +
        "\nUse --force para criar mais um lote.",
    );
    printAccounts();
    return;
  }

  const tokens = { user: userToken, admin: adminToken };
  const byStatus = {};
  for (const [index, [owner, steps]] of PLAN.entries()) {
    const order = await call("POST", "/orders", {
      token: tokens[owner],
      body: buildOrder(index),
    });
    let status = order.status;
    for (const next of steps) {
      const actor = next === "CANCELADO" ? tokens[owner] : adminToken;
      ({ status } = await call("PATCH", `/orders/${order.id}/status`, {
        token: actor,
        body: { status: next },
      }));
    }
    created[owner]++;
    byStatus[status] = (byStatus[status] ?? 0) + 1;
    console.log(
      `ok  #${order.code}  ${order.customer.name.padEnd(22)} ${status.padEnd(18)} (${owner === "user" ? "usuário" : "admin"})`,
    );
  }

  console.log(
    `\n${PLAN.length} pedidos criados: ` +
      Object.entries(byStatus)
        .map(([status, count]) => `${count} ${status}`)
        .join(", "),
  );
  printAccounts();
}

function printAccounts() {
  console.log(
    "\nContas para testar em http://localhost:5173:" +
      `\n  Usuário comum  ${USER.email} / ${USER.password}  (vê só os próprios pedidos)` +
      `\n  Admin          ${ADMIN.email} / ${ADMIN.password}  (vê os pedidos de todos)` +
      (created.user || created.admin
        ? `\n\nNovos pedidos: ${created.user} do usuário comum e ${created.admin} do admin.`
        : ""),
  );
}

main().catch((error) =>
  fail(
    error instanceof ApiError
      ? `A API respondeu ${error.status}: ${error.message}`
      : error.message,
  ),
);
