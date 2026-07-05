import type { StatsResponse } from "../dto/stats"

function formatLastRun(iso: string | null): string {
    if (!iso) return "Never"
    const hours = Math.round((Date.now() - new Date(iso).getTime()) / 3_600_000)
    return `${hours} Hours Ago`
}

function Stat({ label, value }: { label: string; value: string }) {
    return (
        <div>
            <p className="text-label-sm font-body text-secondary uppercase tracking-wide">{label}</p>
            <p className="text-headline-md font-headline text-primary">{value}</p>
        </div>
    )
}

export function StatsBar({ stats }: { stats: StatsResponse }) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-xl bg-surface-container-low border border-outline-variant rounded-lg px-xl py-lg">
            <Stat label="Total" value={`${stats.highlightCount} Highlights`} />
            <div className="hidden md:block w-px h-12 bg-outline-variant" />
            <Stat label="Library" value={`${stats.entryCount} entries`} />
            <div className="hidden md:block w-px h-12 bg-outline-variant" />
            <Stat label="Last run" value={formatLastRun(stats.lastRunTime)} />
        </div>
    )
}
