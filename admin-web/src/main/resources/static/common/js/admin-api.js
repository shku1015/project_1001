/*
 * ② JSP + API: REST API 호출과 토큰 관리 (docs/04-features/09-auth.md 1.1).
 * - Access Token은 페이지의 메모리에만 둔다. 페이지를 옮기면 사라지므로 페이지를 열 때마다 재발급받는다.
 * - Refresh Token은 HttpOnly 쿠키라 스크립트가 읽지 않는다. 재발급 요청에 브라우저가 자동으로 싣는다.
 * - API가 401 TOKEN_EXPIRED를 주면 한 번 재발급받고 원래 요청을 다시 보낸다.
 */
(function (global) {
  'use strict';

  var BASE = '/api/v1';
  var accessToken = null;

  /** 서버 공통 실패 응답을 담는 오류 */
  function ApiError(status, body) {
    var error = (body && body.error) || {};
    this.name = 'ApiError';
    this.status = status;
    this.code = error.code || 'INTERNAL_ERROR';
    this.message = error.message || '일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.';
    this.fieldErrors = error.fieldErrors || [];
  }
  ApiError.prototype = Object.create(Error.prototype);

  async function send(method, path, body, withToken) {
    var headers = { 'Accept': 'application/json' };
    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }
    if (withToken && accessToken) {
      headers['Authorization'] = 'Bearer ' + accessToken;
    }
    var res = await fetch(BASE + path, {
      method: method,
      headers: headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'same-origin'
    });
    var json = null;
    try {
      json = await res.json();
    } catch (e) {
      json = null;
    }
    if (!res.ok) {
      throw new ApiError(res.status, json);
    }
    return json ? json.data : null;
  }

  async function refresh() {
    var token = await send('POST', '/auth/token/refresh', undefined, false);
    accessToken = token.accessToken;
    return token;
  }

  /** 인증이 필요한 API. Access Token이 만료되면 한 번 재발급 후 다시 보낸다 */
  async function request(method, path, body) {
    try {
      return await send(method, path, body, true);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401 && e.code === 'TOKEN_EXPIRED') {
        await refresh();
        return send(method, path, body, true);
      }
      throw e;
    }
  }

  /**
   * 파일 내려받기 (엑셀 등). 토큰을 붙여 받고, 실패하면 파일 대신 온 실패 JSON을 ApiError로 던진다.
   * 파일명은 Content-Disposition의 filename*=UTF-8''... 에서 꺼낸다.
   */
  async function download(path) {
    async function get() {
      return fetch(BASE + path, { headers: accessToken ? { 'Authorization': 'Bearer ' + accessToken } : {}, credentials: 'same-origin' });
    }
    var res = await get();
    if (res.status === 401) {
      var expired = new ApiError(401, await res.json().catch(function () { return null; }));
      if (expired.code !== 'TOKEN_EXPIRED') { throw expired; }
      await refresh();
      res = await get();
    }
    if (!res.ok) {
      throw new ApiError(res.status, await res.json().catch(function () { return null; }));
    }
    var match = /filename\*=UTF-8''([^;]+)/i.exec(res.headers.get('Content-Disposition') || '');
    var href = URL.createObjectURL(await res.blob());
    var a = document.createElement('a');
    a.href = href;
    a.download = match ? decodeURIComponent(match[1]) : 'download.xlsx';
    a.click();
    URL.revokeObjectURL(href);
  }

  global.AdminApi = {
    download: download,
    ApiError: ApiError,
    async login(loginId, password) {
      var token = await send('POST', '/auth/token', { loginId: loginId, password: password }, false);
      accessToken = token.accessToken;
      return token;
    },
    refresh: refresh,
    async logout() {
      try {
        await send('DELETE', '/auth/token', undefined, true);
      } finally {
        accessToken = null;
      }
    },
    me: function () { return request('GET', '/auth/me'); },
    updateMe: function (body) { return request('PUT', '/me', body); },
    post: function (path, body) { return request('POST', path, body); },
    put: function (path, body) { return request('PUT', path, body); },
    del: function (path, body) { return request('DELETE', path, body); },
    patch: function (path, body) { return request('PATCH', path, body); },
    async changePassword(currentPassword, newPassword) {
      var token = await request('PUT', '/me/password', { currentPassword: currentPassword, newPassword: newPassword });
      accessToken = token.accessToken;
      return token;
    },
    get: function (path) { return request('GET', path); }
  };
})(window);
