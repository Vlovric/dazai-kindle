export function RunCard({ title, explanation, onClick }: {
    title: string
    explanation: string
    onClick: () => void
}) {
    return (
        <button className="run-card" onClick={onClick}>
            <h3>{title}</h3>
            <p>{explanation}</p>
        </button>
    )
}
