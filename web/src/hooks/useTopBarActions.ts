import { useEffect, type ReactNode } from "react"
import { useTopBar } from "./useTopBar"

export function useTopBarActions(actions: ReactNode) {
    const { setActions } = useTopBar()

    useEffect(() => {
        setActions(actions)
        return () => setActions(null)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [actions])
}
