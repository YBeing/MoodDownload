import type { PropsWithChildren } from "react";
import { ShellProvider } from "@/domains/shell/store/shell-context";
import { TaskEventsProvider } from "@/app/providers/TaskEventsProvider";
import { CaptureProvider } from "@/domains/capture/store/capture-context";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ShellProvider>
      <TaskEventsProvider>
        <CaptureProvider>{children}</CaptureProvider>
      </TaskEventsProvider>
    </ShellProvider>
  );
}
