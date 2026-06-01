/**
 * Central API catalog for the frontend.
 * Stored on window so api.html also works when opened directly from the filesystem.
 */
(function attachApiCatalog(global) {
  function readBooleanFlag(value, fallback = false) {
    if (typeof value === "boolean") {
      return value;
    }

    if (typeof value === "string") {
      const normalized = value.trim().toLowerCase();
      if (["1", "true", "yes", "on"].includes(normalized)) {
        return true;
      }

      if (["0", "false", "no", "off"].includes(normalized)) {
        return false;
      }
    }

    return fallback;
  }

  const localAuthOnlyMode = readBooleanFlag(global.__LEGAL_LOCAL_AUTH_ONLY__, false);
  const ordersApiEnabled = !readBooleanFlag(
    global.__LEGAL_DISABLE_ORDERS_API__,
    localAuthOnlyMode
  );

  const IMPLEMENTED_API = [
    {
      method: "GET",
      path: "/api/services",
      access: "Public",
      status: "Implemented",
      purpose: "Возвращает список активных юридических услуг для сайта.",
      requestShape: "Без тела запроса.",
      responseShape: "Array<{ id, code, name, shortDescription }>",
      source: "Spring: ServiceController",
      notes:
        "Это endpoint site backend. В локальном auth-only режиме он не обязателен для проверки авторизации и кабинета.",
    },
    {
      method: "POST",
      path: "/api/auth/register",
      access: "Public",
      status: "Implemented",
      purpose: "Регистрирует нового клиента и сразу возвращает auth-session.",
      requestShape: "{ fullName, email, phone, companyName, password }",
      responseShape: "{ accessToken, refreshToken, tokenType, expiresIn, user }",
      source: "Auth service: RegController",
      notes:
        "Это текущий локальный контракт auth-service на 8081. Фронт сохраняет access/refresh token и профиль пользователя в session-state.",
    },
    {
      method: "POST",
      path: "/api/auth/login",
      access: "Public",
      status: "Implemented",
      purpose: "Логинит пользователя и возвращает токены вместе с профилем.",
      requestShape: "{ email, password }",
      responseShape: "{ accessToken, refreshToken, tokenType, expiresIn, user }",
      source: "Auth service: LoginController",
      notes:
        "Локально фронт работает с отдельным auth-service, поэтому после reload опирается на сохранённые токены и /api/auth/me.",
    },
    {
      method: "POST",
      path: "/api/auth/refresh",
      access: "Public",
      status: "Implemented",
      purpose: "Обменивает refresh token на новую auth-session.",
      requestShape: "{ refreshToken }",
      responseShape: "{ accessToken, refreshToken, tokenType, expiresIn, user }",
      source: "Auth service: RefreshController",
      notes:
        "Хороший enterprise-подход: access token короткоживущий, а refresh token позволяет не выкидывать пользователя лишний раз.",
    },
    {
      method: "GET",
      path: "/api/auth/me",
      access: "Auth",
      status: "Implemented",
      purpose: "Возвращает профиль текущего пользователя.",
      requestShape: "Authorization: Bearer <accessToken>.",
      responseShape: "{ id, fullName, email, phone, companyName, role }",
      source: "Auth service: MeController",
      notes:
        "Во frontend этот endpoint обновляет только данные пользователя и не должен затирать сохранённые токены в local state.",
    },
    {
      method: "POST",
      path: "/api/auth/logout",
      access: "Auth",
      status: "Implemented",
      purpose: "Инвалидирует refresh token и завершает клиентскую auth-session.",
      requestShape: "{ refreshToken }",
      responseShape: "boolean",
      source: "Auth service: LogoutController",
      notes:
        "Именно этот endpoint используется одинаково из шапки сайта, кабинета и страницы заказа, чтобы logout вел себя предсказуемо.",
    },
  ];

  const PLANNED_API = [
    {
      method: "POST",
      path: "/api/client/applications",
      access: "Auth",
      status: "Planned",
      purpose: "Создание клиентской заявки с multipart-документами.",
      requestShape:
        "multipart/form-data: serviceCode, clientName, contact, companyName, description, documents[]",
      responseShape: "{ id, status, trackingCode }",
      source: "Frontend: orders-api.js",
      notes: "Нужен для формы отправки заявок с сайта.",
    },
    {
      method: "GET",
      path: "/api/client/orders",
      access: "Auth",
      status: "Planned",
      purpose: "Возвращает список заказов клиента в кабинете.",
      requestShape: "Без тела запроса.",
      responseShape: "Array<OrderSummary>",
      source: "Frontend: orders-api.js",
      notes: "Используется страницей личного кабинета.",
    },
    {
      method: "GET",
      path: "/api/client/orders/:orderId",
      access: "Auth",
      status: "Planned",
      purpose: "Возвращает карточку конкретного заказа.",
      requestShape: "orderId в path variable.",
      responseShape: "OrderDetails",
      source: "Frontend: orders-api.js",
      notes: "Нужен для `order.html`.",
    },
    {
      method: "POST",
      path: "/api/client/orders/:orderId/rework",
      access: "Auth",
      status: "Planned",
      purpose: "Отправляет заказ юристу на доработку с комментарием клиента.",
      requestShape: "{ comment }",
      responseShape: "OrderDetails или { success: true }",
      source: "Frontend: orders-api.js",
      notes: "Хороший кандидат на отдельный application service и историю статусов.",
    },
    {
      method: "PATCH",
      path: "/api/client/orders/:orderId",
      access: "Auth",
      status: "Planned",
      purpose: "Редактирует клиентскую заявку из карточки заказа.",
      requestShape: "{ serviceCode, clientName, contact, companyName, description }",
      responseShape: "OrderDetails | { success: true }",
      source: "Frontend: orders-api.js",
      notes: "Используется клиентской модалкой редактирования заявки.",
    },
    {
      method: "DELETE",
      path: "/api/client/orders/:orderId",
      access: "Auth",
      status: "Planned",
      purpose: "Удаляет клиентскую заявку после подтверждения.",
      requestShape: "Без тела запроса.",
      responseShape: "void | { success: true }",
      source: "Frontend: orders-api.js",
      notes: "После удаления frontend уводит пользователя обратно в кабинет.",
    },
    {
      method: "POST",
      path: "/api/auth/google/login",
      access: "Public",
      status: "Implemented",
      purpose: "Проверяет Google credential и либо логинит пользователя, либо переводит во flow дозаполнения профиля.",
      requestShape: "{ credential }",
      responseShape: "AuthResponce | { status, auth? , authResponce? , flowToken? , profile? , googleResponce? }",
      source: "Frontend: auth-api.js",
      notes: "При PROFILE_COMPLETION_REQUIRED фронт открывает complete-profile.html и продолжает flow через отдельный endpoint.",
    },
    {
      method: "POST",
      path: "/api/auth/google/complete",
      access: "Public",
      status: "Implemented",
      purpose: "Завершает регистрацию Google-пользователя через flowToken и создаёт обычную app-session.",
      requestShape: "{ flowToken, fullName, password }",
      responseShape: "{ accessToken, refreshToken, tokenType, expiresIn, user }",
      source: "Frontend: auth-api.js",
      notes: "Используется страницей complete-profile.html после Google first-login.",
    },
    {
      method: "DELETE",
      path: "/api/auth/account",
      access: "Auth",
      status: "Planned",
      purpose: "Удаляет аккаунт в более REST-style контракте.",
      requestShape: "Без тела запроса.",
      responseShape: "{ success: true }",
      source: "Frontend: auth-api.js",
      notes: "Лучше совпадает с семантикой удаления ресурса.",
    },
    {
      method: "GET",
      path: "/api/staff/board/tasks",
      access: "Staff",
      status: "Planned",
      purpose: "Список задач для доски юриста.",
      requestShape: "Без тела запроса.",
      responseShape: "Array<TaskCard> | { items: TaskCard[] }",
      source: "Frontend: staff-api.js",
      notes: "Это уже будущий lawyer dashboard слой.",
    },
    {
      method: "GET",
      path: "/api/staff/board/tasks/:taskId",
      access: "Staff",
      status: "Planned",
      purpose: "Детали конкретной задачи на доске.",
      requestShape: "taskId в path variable.",
      responseShape: "TaskDetails",
      source: "Frontend: staff-api.js",
      notes: "Обычно это отдельный read-model endpoint.",
    },
    {
      method: "PATCH",
      path: "/api/staff/board/tasks/:taskId/status",
      access: "Staff",
      status: "Planned",
      purpose: "Меняет статус задачи на доске.",
      requestShape: "{ status }",
      responseShape: "TaskDetails",
      source: "Frontend: staff-api.js",
      notes: "По смыслу похож на update status command.",
    },
    {
      method: "PATCH",
      path: "/api/staff/board/tasks/:taskId",
      access: "Staff",
      status: "Planned",
      purpose: "Редактирует данные заявки с доски юриста.",
      requestShape: "{ serviceCode, clientName, contact, companyName, description }",
      responseShape: "TaskDetails",
      source: "Frontend: staff-api.js",
      notes: "Нужен для staff-модалки редактирования заявки.",
    },
    {
      method: "POST",
      path: "/api/staff/board/tasks/:taskId/reject",
      access: "Staff",
      status: "Planned",
      purpose: "Отклоняет заявку с обязательной причиной юриста.",
      requestShape: "{ reason }",
      responseShape: "TaskDetails | { success: true }",
      source: "Frontend: staff-api.js",
      notes: "После отклонения карточка исчезает из активных колонок доски.",
    },
    {
      method: "DELETE",
      path: "/api/staff/board/tasks/:taskId",
      access: "Staff",
      status: "Planned",
      purpose: "Удаляет заявку с доски юриста.",
      requestShape: "Без тела запроса.",
      responseShape: "void | { success: true }",
      source: "Frontend: staff-api.js",
      notes: "Используется staff-action удаления карточки.",
    },
    {
      method: "POST",
      path: "/api/staff/board/tasks/:taskId/comments",
      access: "Staff",
      status: "Planned",
      purpose: "Добавляет комментарий к задаче.",
      requestShape: "{ body }",
      responseShape: "TaskDetails",
      source: "Frontend: staff-api.js",
      notes: "Хорошо ложится на отдельную сущность comment/history.",
    },
  ];

  global.API_CATALOG = {
    IMPLEMENTED_API,
    PLANNED_API,
    API_SUMMARY: {
      implementedCount: IMPLEMENTED_API.length,
      plannedCount: PLANNED_API.length,
      backendStyle: localAuthOnlyMode
        ? "Локально поднят отдельный auth-service с access/refresh token и профилем через /api/auth/me."
        : "Фронт умеет жить и с отдельным auth-service, и с общим backend-контрактом.",
      frontendStyle: ordersApiEnabled
        ? "Фронт читает runtime-флаги из local-dev-config.js и использует включённый orders API."
        : "Фронт читает runtime-флаги из local-dev-config.js и честно отключает orders API без скрытого fallback-а.",
    },
  };
})(window);
