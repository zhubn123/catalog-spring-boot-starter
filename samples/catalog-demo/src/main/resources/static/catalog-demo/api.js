export function createApi({ baseUrl, logApi }) {
    // 统一处理日志和错误记录，页面模块只保留业务逻辑。
    const request = async (method, url, config = {}) => {
        try {
            const response = await axios({
                method,
                url: baseUrl + url,
                ...config
            });
            logApi(method.toUpperCase(), url, true, response.status);
            return response.data;
        } catch (error) {
            logApi(method.toUpperCase(), url, false, error.response?.status || 0);
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
