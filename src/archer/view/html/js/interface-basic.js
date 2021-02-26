layui.use(["element", "form", "laytpl"], function () {
    const element = layui.element;

    element.tab({
        headerElem: "#sidebar-tab>.sidebar-tab-item",
        bodyElem: "#content>.content-item",
    });

    element.on("tab(sidebar-tab)", function (data) {
        let sidebarTabId = data.elem.prevObject.attr("id");
        if (sidebarTabId === "sidebar-repo-home") {
            loadRepoContent();
        }
    });

    $("#sidebar-repo-close").click(function () {
        try {
            javaApi.closeRepository();
        } catch (error) {
            logError(error);
        }
    });
});
