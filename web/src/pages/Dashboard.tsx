import { useStats } from "../hooks/useStats"
import { StatsBar } from "../components/StatsBar"
import { RunCard } from "../components/RunCard"

const RUN_TYPES = [
    { title: "Full Run", explanation: "Parse clippings and render output using a template." },
    { title: "Generate Calibration File", explanation: "Fit Kindle locations to byte offsets from a book." },
    { title: "Headings Only", explanation: "Render headings without processing clippings." },
]

export function Dashboard() {
    const { data: stats, loading, error } = useStats()

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <section className="mb-2xl">
                <div className="flex items-center gap-sm mb-lg">
                    <span className="w-8 h-1 bg-primary rounded-full" />
                    <h2 className="font-headline text-headline-md text-primary">Start New Run</h2>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-lg">
                    {RUN_TYPES.map((run) => (
                        <RunCard key={run.title} {...run} onClick={() => {/* navigate to run screen */ }} />
                    ))}
                </div>
            </section>

            <section>
                <div className="flex items-center gap-sm mb-lg">
                    <span className="w-8 h-1 bg-secondary rounded-full" />
                    <h2 className="font-headline text-headline-md text-primary">Stats</h2>
                </div>
                {loading && <p className="font-body text-body-md text-secondary">Loading…</p>}
                {error && <p className="font-body text-body-md text-error">Failed to load stats: {error}</p>}
                {stats && <StatsBar stats={stats} />}
            </section>
        </main>
    )
}
