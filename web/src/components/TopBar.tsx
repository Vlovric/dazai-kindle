import { useTopBar } from "../hooks/useTopBar"

export function TopBar() {
    const { searchQuery, setSearchQuery, actions } = useTopBar()

    return (
        <header className="flex items-center justify-between gap-md px-lg py-sm border-b border-outline-variant bg-surface">
            <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search"
                className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface placeholder:text-secondary focus:outline-none focus:border-primary w-64"
            />
            <div className="flex items-center gap-sm">{actions}</div>
        </header>
    )
}
