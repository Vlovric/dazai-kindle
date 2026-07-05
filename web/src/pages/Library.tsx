import { useMemo, useState } from "react"
import { useTopBarActions } from "../hooks/useTopBarActions"

export function Library() {
    const [searchQuery, setSearchQuery] = useState("")

    const topBarActions = useMemo(() => (
        <>
            <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search"
                className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface placeholder:text-secondary focus:outline-none focus:border-primary w-64 mr-auto"
            />
            <button className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-label-md font-body text-primary hover:border-primary transition-colors">
                View clippings file
            </button>
        </>
    ), [searchQuery])

    useTopBarActions(topBarActions)

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <h2 className="font-headline text-headline-md text-primary">Library</h2>
        </main>
    )
}
