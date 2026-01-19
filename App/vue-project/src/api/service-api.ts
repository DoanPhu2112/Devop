import { fetchApi } from "../config/fetch-api"

export const serviceApi = {
    oracle() {
        alert('call oracle api')
        return fetchApi.get('/api/oracle')
    }
} 