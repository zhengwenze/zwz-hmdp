const DEFAULT_HEADERS = {
  Accept: 'application/json',
}

function trimSlash(value) {
  return value.replace(/\/+$/, '')
}

function buildUrl(baseUrl, path, query) {
  const search = new URLSearchParams()
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    search.append(key, String(value))
  })

  const queryString = search.toString()
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  if (!baseUrl) {
    return queryString ? `${normalizedPath}?${queryString}` : normalizedPath
  }

  const prefix = trimSlash(baseUrl)
  return queryString
    ? `${prefix}${normalizedPath}?${queryString}`
    : `${prefix}${normalizedPath}`
}

export function asNumber(value) {
  if (value === '' || value === null || value === undefined) {
    return undefined
  }

  const parsed = Number(value)
  return Number.isNaN(parsed) ? undefined : parsed
}

export function asBoolean(value) {
  if (value === true || value === false) {
    return value
  }

  if (value === 'true') {
    return true
  }

  if (value === 'false') {
    return false
  }

  return undefined
}

export function cloneWithoutEmpty(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== '' && value !== undefined),
  )
}

export function prettify(value) {
  if (value === undefined) {
    return ''
  }

  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

export function resolveAssetUrl(assetBase, rawPath) {
  if (!rawPath) {
    return ''
  }

  if (/^https?:\/\//.test(rawPath)) {
    return rawPath
  }

  if (!assetBase) {
    return rawPath
  }

  return `${trimSlash(assetBase)}${rawPath.startsWith('/') ? '' : '/'}${rawPath}`
}

export function createApiClient({ getBaseUrl, getToken, onLog, onUnauthorized }) {
  return {
    async request({ method = 'GET', path, query, body, formData, headers }) {
      const url = buildUrl(getBaseUrl?.(), path, query)
      const requestHeaders = new Headers({ ...DEFAULT_HEADERS, ...(headers || {}) })
      const token = getToken?.()

      if (token) {
        requestHeaders.set('authorization', token)
      }

      let payload = undefined
      if (formData) {
        payload = formData
        requestHeaders.delete('Content-Type')
      } else if (body !== undefined) {
        payload = JSON.stringify(body)
        requestHeaders.set('Content-Type', 'application/json')
      }

      const startedAt = Date.now()

      try {
        const response = await fetch(url, {
          method,
          headers: requestHeaders,
          body: payload,
        })

        const contentType = response.headers.get('content-type') || ''
        let parsedBody = null
        if (contentType.includes('application/json')) {
          parsedBody = await response.json()
        } else {
          const text = await response.text()
          parsedBody = text || null
        }

        const entry = {
          ok: response.ok,
          status: response.status,
          method,
          path,
          url,
          durationMs: Date.now() - startedAt,
          body: parsedBody,
          headers: Object.fromEntries(response.headers.entries()),
        }

        if (response.status === 401) {
          onUnauthorized?.(entry)
        }

        onLog?.(entry)
        return entry
      } catch (error) {
        const entry = {
          ok: false,
          status: 0,
          method,
          path,
          url,
          durationMs: Date.now() - startedAt,
          body: null,
          error: error instanceof Error ? error.message : String(error),
          headers: {},
        }

        onLog?.(entry)
        return entry
      }
    },
  }
}
