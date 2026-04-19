import { useShellContext } from "@/domains/shell/store/shell-context";

export function useShell() {
  return useShellContext();
}
