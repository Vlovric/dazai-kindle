import { apiGet } from "./client";
import type { StatsResponse } from "../dto/stats";

export function getStats(): Promise<StatsResponse>{
    return apiGet<StatsResponse>("/stats");
}