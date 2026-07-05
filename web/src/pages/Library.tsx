import { useTopBarActions } from "../hooks/useTopBarActions"

export function Library() {
    useTopBarActions(
        <button className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-label-md font-body text-primary hover:border-primary transition-colors">
            View clippings file
        </button>
    )

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <h2 className="font-headline text-headline-md text-primary">Library</h2>
        </main>
    )
}
