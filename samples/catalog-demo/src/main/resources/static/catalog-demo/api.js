const MAX_LOG_TEXT_LENGTH = 4000;

function hasLogValue(value) {
    if (value === null || value === undefined) {
        return false;
    }
    if (typeof value === "string") {
        return value.trim().length > 0;
    }
    if (Array.isArray(value)) {
        return value.length > 0;
    }
    if (typeof value === "object") {
        return Object.keys(value).length > 0;
    }
    return true;
}

function truncateLogText(text, maxLength = MAX_LOG_TEXT_LENGTH) {
    if (text.length <= maxLength) {
        return text;
    }
    return `${text.slice(0, maxLength)}\n...<truncated>`;
}

function serializeLogValue(value) {
    if (!hasLogValue(value)) {
        return "";
    }
    if (typeof value === "string") {
        return truncateLogText(value);
    }
    try {
        return truncateLogText(JSON.stringify(value, null, 2));
    } catch (_error) {
        return truncateLogText(String(value));
    }
}

function buildLogEntry(method, baseUrl, url, config, status, statusText, durationMs, success, responseData, error) {
    return {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        time: new Date().toLocaleTimeString(),
        method,
        url,
        fullUrl: baseUrl + url,
        success,
        status,
        statusText: statusText || "",
        durationMs,
        requestParamsText: serializeLogValue(config.params),
        requestBodyText: serializeLogValue(config.data),
        responseBodyText: serializeLogValue(responseData),
        errorMessage: error ? serializeLogValue(error.message || error.code || error) : ""
    };
}

export function createApi({ baseUrl, logApi }) {
    // 统一记录请求日志，方便在 sample 页里直接看到入参、出参和耗时。
    const request = async (method, url, config = {}) => {
        const startedAt = Date.now();
        const normalizedMethod = method.toUpperCase();
        try {
            const response = await axios({
                method,
                url: baseUrl + url,
                ...config
            });
            logApi(buildLogEntry(
                normalizedMethod,
                baseUrl,
                url,
                config,
                response.status,
                response.statusText,
                Date.now() - startedAt,
                true,
                response.data,
                null
            ));
            return response.data;
        } catch (error) {
            logApi(buildLogEntry(
                normalizedMethod,
                baseUrl,
                url,
                config,
                error.response?.status || 0,
                error.response?.statusText || "",
                Date.now() - startedAt,
                false,
                error.response?.data,
                error
            ));
            throw error;
        }
    };

    return {
        get(url) {
            return request("get", url);
        },
        post(url, params) {
            return request("post", url, { data: null, params });
        },
        postJson(url, data) {
            return request("post", url, { data });
        },
        put(url, data) {
            return request("put", url, { data });
        }
    };
}

export function extractErrorMessage(error, fallback = "请求失败") {
    const responseData = error?.response?.data;
    if (typeof responseData?.message === "string" && responseData.message.trim()) {
        return responseData.message;
    }
    if (typeof responseData?.error === "string" && responseData.error.trim()) {
        return responseData.error;
    }
    if (typeof error?.message === "string" && error.message.trim()) {
        return error.message;
    }
    return fallback;
}

export function unwrapResultData(result) {
    // 兼容 sample 中少量包装响应和 catalog 原生响应并存的情况。
    if (result && typeof result === "object" && Object.prototype.hasOwnProperty.call(result, "data")) {
        return result.data;
    }
    return result;
}