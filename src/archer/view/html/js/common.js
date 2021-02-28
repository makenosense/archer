function warn(msg) {
    javaApi.warn(msg);
}

function logError(error) {
    try {
        javaApi.error(error.toString());
    } finally {
        console.log(error);
    }
}

function switchToSidebarTab(sidebarTabId) {
    $("#sidebar-tab").find("#" + sidebarTabId).click();
}

function switchToSidebarTabWithDelay(sidebarTabId, delay = 100) {
    setTimeout(function () {
        switchToSidebarTab(sidebarTabId);
    }, delay);
}
