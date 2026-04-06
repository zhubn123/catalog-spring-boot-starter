export function createApi({ baseUrl, logApi }) {
    return {
        async get(url) {
            try {
                const response = await axios.get(baseUrl + url);
                logApi("GET", url, true, response.status);
                return response.data;
            } catch (error) {
                logApi("GET", url, false, error.response?.status || 0);
                throw error;
            }
        },
        async post(url, params) {
            try {
                const response = await axios.post(baseUrl + url, null, { params });
                logApi("POST", url, true, response.status);
                return response.data;
            } catch (error) {
                logApi("POST", url, false, error.response?.status || 0);
                throw error;
            }
        },
        async put(url, data) {
            try {
                const response = await axios.put(baseUrl + url, data);
                logApi("PUT", url, true, response.status);
                return response.data;
            } catch (error) {
                logApi("PUT", url, false, error.response?.status || 0);
                throw error;
            }
        }
    };
}
