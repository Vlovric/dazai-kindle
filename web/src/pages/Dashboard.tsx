import { useStats } from "../hooks/useStats"
import { StatsBar } from "../components/StatsBar"
import { RunCard } from "../components/RunCard"

const RUN_TYPES = [
    { title: "Full Run", explanation: "Parse clippings and render output." },
    { title: "Generate Calibration File", explanation: "Fit locations from a book." },
    { title: "Headings Only", explanation: "Render headings without clippings." },
]

export function Dashboard() {
    const { data: stats, loading, error } = useStats()

    return (
        <section>
            <h2>Start New Run</h2>
            <div className="run-cards">
                {RUN_TYPES.map((run) => (
                    <RunCard key={run.title} {...run} onClick={() => {/* navigate to run screen */ }} />
                ))}
            </div>

            <h2>Stats</h2>
            {loading && <p>Loading…</p>}
            {error && <p>Failed to load stats: {error}</p>}
            {stats && <StatsBar stats={stats} />}
        </section>
    )
}
