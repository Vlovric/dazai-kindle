import type { StatsResponse } from "../dto/stats"

function formatLastRun(iso: string | null): string {
    if (!iso) return "Never"
    const hours = Math.round((Date.now() - new Date(iso).getTime()) / 3_600_000)
    return `${hours} Hours Ago`
}

export function StatsBar({ stats }: { stats: StatsResponse }) {
    return (
        <div className="stats-bar">
            <div>
                <span>Total</span>
                <strong>{stats.highlightCount} Highlights</strong>
            </div>
            <div>
                <span>Library</span>
                <strong>{stats.entryCount} entries</strong>
            </div>
            <div>
                <span>Last run</span>
                <strong>{formatLastRun(stats.lastRunTime)}</strong>
            </div>
        </div>
    )
}
