import { Outlet } from "react-router-dom";
import { Sidebar } from "@/domains/shell/components/Sidebar";
import { StatusFooter } from "@/domains/shell/components/StatusFooter";
import { TaskCreateModal } from "@/domains/shell/components/TaskCreateModal";
import { TaskDetailDrawer } from "@/domains/shell/components/TaskDetailDrawer";
import { TitleBar } from "@/domains/shell/components/TitleBar";
import { ToastViewport } from "@/shared/components/ToastViewport";
import { CapturePromptViewport } from "@/domains/capture/components/CapturePromptViewport";

export function AppShell() {
  return (
    <div className="app-shell">
      <TitleBar />

      <div className="app-body">
        <Sidebar />
        <main className="content-panel">
          <div className="page-scroll">
            <Outlet />
          </div>
        </main>
      </div>

      <StatusFooter />
      <TaskCreateModal />
      <TaskDetailDrawer />
      <CapturePromptViewport />
      <ToastViewport />
    </div>
  );
}
