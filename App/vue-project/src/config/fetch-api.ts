import axios from 'axios';

export const fetchApi = axios.create({
    baseURL: 'http://localhost:8080',
})

fetchApi.interceptors.response.use(
    res => res.data,
    err => Promise.reject(err)
)