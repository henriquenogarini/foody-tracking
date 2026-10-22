#!/usr/bin/env node
// Smoke test of the running API against the technical test requirements.
// Usage: node scripts/smoke-test.mjs [apiUrl]   (default http://localhost:8080/api/v1, Node 22+)
// Creates two throwaway users and a few orders; exits with 1 if any check fails.

const API = (process.argv[2] ?? "http://localhost:8080/api/v1").replace(
  /\/$/,
  "",
);
const ORIGIN = "http://localhost:5173";
const run = Date.now().toString(36);
let failures = 0;

async function call(method, path, { token, body, headers = {} } = {}) {
  const res = await fetch(API + path, {
    method,
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  return {
    status: res.status,
    headers: res.headers,
    data: text ? JSON.parse(text) : null,
  };
}

function check(name, ok, detail = "") {
  console.log(
    `${ok ? "ok  " : "FAIL"}  ${name}${ok || !detail ? "" : `  → ${detail}`}`,
  );
  if (!ok) failures++;
}

const section = (title) => console.log(`\n${title}`);

const order = {
  customer: {
    name: "José da Silva",
    phone: "+5511987521144",
    email: "jose@exemplo.com",
  },
  items: [
    { description: "Pizza calabresa G", quantity: 1, unitPrice: 59.9 },
    { description: "Guaraná 2L", quantity: 2, unitPrice: 14.95 },
  ],
  deliveryAddress: {
    street: "Rua José Neves",
    number: "590",
    complement: "Sala 03",
    neighborhood: "Vila São Paulo",
    city: "São Paulo",
    state: "SP",
    postalCode: "04650-140",
  },
  notes: "Sem cebola.",
};

section("Infraestrutura");
const health = await fetch(
  API.replace(/\/api\/v1$/, "") + "/actuator/health",
).catch(() => null);
check(
  "API no ar (/actuator/health)",
  health?.ok === true,
  `não respondeu em ${API}`,
);
if (!health?.ok) process.exit(1);
const preflight = await fetch(API + "/orders", {
  method: "OPTIONS",
  headers: {
    Origin: ORIGIN,
    "Access-Control-Request-Method": "GET",
    "Access-Control-Request-Headers": "authorization",
  },
});
check(
  `CORS libera o frontend (${ORIGIN})`,
  preflight.headers.get("access-control-allow-origin") === ORIGIN,
  `allow-origin = ${preflight.headers.get("access-control-allow-origin")}`,
);

section("Autenticação");
const anonymous = await call("GET", "/orders");
check(
  "Sem token, /orders responde 401",
  anonymous.status === 401,
  `HTTP ${anonymous.status}`,
);

const alice = {
  name: "Alice Smoke",
  email: `alice-${run}@teste.local`,
  password: "senha-forte-1",
};
const registered = await call("POST", "/auth/register", { body: alice });
check(
  "Cadastro (nome, e-mail, senha) → 201",
  registered.status === 201 &&
    registered.data?.email === alice.email &&
    !("password" in (registered.data ?? {})),
  `HTTP ${registered.status}`,
);
const duplicate = await call("POST", "/auth/register", { body: alice });
check(
  "E-mail repetido → 409",
  duplicate.status === 409,
  `HTTP ${duplicate.status}`,
);
const invalidSignup = await call("POST", "/auth/register", {
  body: { name: "", email: "x", password: "1" },
});
check(
  "Cadastro inválido → 400 com erros por campo",
  invalidSignup.status === 400 &&
    (invalidSignup.data?.errors?.length ?? 0) >= 3,
  `HTTP ${invalidSignup.status}`,
);

const wrong = await call("POST", "/auth/login", {
  body: { email: alice.email, password: "errada-123" },
});
check(
  "Login com senha errada → 401",
  wrong.status === 401,
  `HTTP ${wrong.status}`,
);
const login = await call("POST", "/auth/login", {
  body: { email: alice.email, password: alice.password },
});
const token = login.data?.accessToken;
check(
  "Login (e-mail e senha) → token JWT",
  login.status === 200 && typeof token === "string",
  `HTTP ${login.status}`,
);
const me = await call("GET", "/auth/me", { token });
check(
  "/auth/me devolve o usuário logado",
  me.data?.email === alice.email,
  `HTTP ${me.status}`,
);
const forged = await call("GET", "/orders", { token: "token.invalido.123" });
check("Token inválido → 401", forged.status === 401, `HTTP ${forged.status}`);

section("Pedidos");
const created = await call("POST", "/orders", { token, body: order });
const id = created.data?.id;
check(
  "Criar pedido (cliente, itens, endereço) → 201",
  created.status === 201 && typeof id === "string",
  `HTTP ${created.status} ${JSON.stringify(created.data)?.slice(0, 160)}`,
);
check(
  "Pedido nasce como RECEBIDO",
  created.data?.status === "RECEBIDO",
  created.data?.status,
);
check(
  "Total calculado pela API (59,90 + 2 × 14,95 = 89,80)",
  created.data?.total === 89.8,
  String(created.data?.total),
);

const noItems = await call("POST", "/orders", {
  token,
  body: { ...order, items: [] },
});
check(
  "Pedido sem itens → 400 VALIDATION_ERROR",
  noItems.status === 400 && noItems.data?.code === "VALIDATION_ERROR",
  `HTTP ${noItems.status}`,
);

const list = await call("GET", "/orders?size=100", { token });
check(
  "Listar pedidos inclui o pedido criado, com status atual",
  list.status === 200 &&
    list.data?.content?.some((o) => o.id === id && o.status === "RECEBIDO"),
  `HTTP ${list.status}`,
);
const byStatus = await call("GET", "/orders?status=RECEBIDO", { token });
check(
  "Filtrar por status",
  byStatus.data?.content?.every((o) => o.status === "RECEBIDO"),
  JSON.stringify(byStatus.data?.content?.map((o) => o.status)),
);
const byCode = await call("GET", `/orders?q=%23${created.data?.code}`, {
  token,
});
check(
  "Buscar por código (#1001)",
  byCode.data?.content?.[0]?.id === id,
  `HTTP ${byCode.status}`,
);

const found = await call("GET", `/orders/${id}`, { token });
check(
  "Buscar pedido por ID",
  found.status === 200 && found.data?.items?.length === 2,
  `HTTP ${found.status}`,
);
const missing = await call(
  "GET",
  "/orders/00000000-0000-4000-8000-000000000000",
  { token },
);
check("ID inexistente → 404", missing.status === 404, `HTTP ${missing.status}`);

section("Status");
for (const status of ["EM_PREPARO", "SAIU_PARA_ENTREGA", "ENTREGUE"]) {
  const moved = await call("PATCH", `/orders/${id}/status`, {
    token,
    body: { status },
  });
  check(
    `Atualizar status → ${status}`,
    moved.status === 200 && moved.data?.status === status,
    `HTTP ${moved.status}`,
  );
}
const backwards = await call("PATCH", `/orders/${id}/status`, {
  token,
  body: { status: "EM_PREPARO" },
});
check(
  "Transição inválida (ENTREGUE → EM_PREPARO) → 409",
  backwards.status === 409 &&
    backwards.data?.code === "ORDER_INVALID_TRANSITION",
  `HTTP ${backwards.status}`,
);
const unknownStatus = await call("PATCH", `/orders/${id}/status`, {
  token,
  body: { status: "VOANDO" },
});
check(
  "Status desconhecido → 400",
  unknownStatus.status === 400,
  `HTTP ${unknownStatus.status}`,
);

const second = await call("POST", "/orders", { token, body: order });
const cancelled = await call("PATCH", `/orders/${second.data?.id}/status`, {
  token,
  body: { status: "CANCELADO" },
});
check(
  "Cancelar pedido RECEBIDO → CANCELADO",
  cancelled.data?.status === "CANCELADO",
  `HTTP ${cancelled.status}`,
);
const revived = await call("PATCH", `/orders/${second.data?.id}/status`, {
  token,
  body: { status: "EM_PREPARO" },
});
check(
  "Pedido cancelado não volta (→ 409)",
  revived.status === 409,
  `HTTP ${revived.status}`,
);

const history = await call("GET", `/orders/${id}/history`, { token });
check(
  "Histórico com as 4 etapas, em ordem, e quem alterou",
  JSON.stringify(history.data?.map((h) => h.toStatus)) ===
    JSON.stringify([
      "RECEBIDO",
      "EM_PREPARO",
      "SAIU_PARA_ENTREGA",
      "ENTREGUE",
    ]) && history.data?.every((h) => h.changedByName === alice.name),
  JSON.stringify(history.data?.map((h) => h.toStatus)),
);

section("Isolamento entre usuários");
const bob = {
  name: "Bob Smoke",
  email: `bob-${run}@teste.local`,
  password: "senha-forte-2",
};
await call("POST", "/auth/register", { body: bob });
const bobToken = (await call("POST", "/auth/login", { body: bob })).data
  ?.accessToken;
const peek = await call("GET", `/orders/${id}`, { token: bobToken });
check(
  "Outro usuário não vê o pedido (→ 404)",
  peek.status === 404,
  `HTTP ${peek.status}`,
);
const bobList = await call("GET", "/orders", { token: bobToken });
check(
  "Lista do outro usuário vem vazia",
  bobList.data?.totalElements === 0,
  `${bobList.data?.totalElements} pedidos`,
);

console.log(
  `\n${failures ? `${failures} verificação(ões) falharam` : "Todas as verificações passaram"}`,
);
process.exit(failures ? 1 : 0);
