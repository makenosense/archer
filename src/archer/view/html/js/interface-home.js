let repoOpsOnselect = $("#repo-ops-onselect");
let repoNav = $("#repo-nav");
let repoNavPath = $("#repo-nav-path");
let repoContentTable = $("#repo-content-table");
let repoContentCheckAll = $("#repo-content-check-all");
let repoContentTableBody = $("#repo-content-table > tbody");
let statusCount = $("#repo-content-status-count > span");


function repoContentLoading() {
    repoOpsOnselect.hide();
    switchRepoNavOps("repo-nav-ops-refresh", false);
    repoContentCheckAll.prop("checked", false);
    statusCount.html($("#repo-content-status-count-loading-tpl").html());
    repoContentTableBody.html($("#repo-content-table-loading-tpl").html());
}

function switchRepoNavOps(opID, status) {
    let navOp = $("#" + opID);
    let disabledClass = "repo-nav-ops-disabled";
    if (status) {
        navOp.removeClass(disabledClass);
    } else {
        navOp.addClass(disabledClass);
    }
}

function sumRepoNavPathNodeWidth() {
    let sum = 0;
    repoNavPath.children().each(function () {
        sum += $(this).outerWidth(true);
    });
    return sum;
}

function adjustRepoNavPath() {
    try {
        let pathNodeList = javaApi.getPathNodeListAsArray();
        repoNavPath.html(layui.laytpl($("#repo-nav-path-node-tpl").html()).render(pathNodeList));
        let pathNodeEllipsis = $("#repo-nav-path-node-ellipsis");
        pathNodeEllipsis.hide();
        while (repoNavPath.width() < sumRepoNavPathNodeWidth()) {
            pathNodeEllipsis.show();
            $("#repo-nav-path-node-flex").children().first().remove();
        }
    } catch (error) {
        logError(error);
    }
}

function loadRepoContent() {
    try {
        repoContentLoading();
        setTimeout(function () {
            javaApi.loadRepositoryContent();
        }, 1);
    } catch (error) {
        logError(error);
    }
}

function fillRepoContent() {
    try {
        switchRepoNavOps("repo-nav-ops-previous", javaApi.hasPrevious());
        switchRepoNavOps("repo-nav-ops-next", javaApi.hasNext());
        switchRepoNavOps("repo-nav-ops-parent", javaApi.hasParent());
        adjustRepoNavPath();
        fillRepoContentTable();
    } catch (error) {
        logError(error);
    }
}

function fillRepoContentTable() {
    let entryList = javaApi.getEntryListAsArray();
    statusCount.html(entryList.length);
    if (entryList.length > 0) {
        repoContentTableBody.html(layui.laytpl($("#repo-content-table-tr-tpl").html()).render(entryList));
    } else {
        repoContentTableBody.html($("#repo-content-table-empty-tpl").html());
    }
}

function allChecked() {
    let result = true;
    $(".repo-content-check").each(function () {
        if (!$(this).prop("checked")) {
            result = false;
        }
    });
    return result;
}

function allNotChecked() {
    let result = true;
    $(".repo-content-check").each(function () {
        if ($(this).prop("checked")) {
            result = false;
        }
    });
    return result;
}

function goPath(pathString) {
    try {
        javaApi.goPath(pathString);
    } catch (error) {
        logError(error);
    }
}

function openDir(name) {
    try {
        javaApi.openDir(name);
    } catch (error) {
        logError(error);
    }
}

$(function () {
    repoNav.delegate("#repo-nav-ops-previous:not(.repo-nav-ops-disabled)", "click", function () {
        try {
            javaApi.goPrevious();
        } catch (error) {
            logError(error);
        }
    });

    repoNav.delegate("#repo-nav-ops-next:not(.repo-nav-ops-disabled)", "click", function () {
        try {
            javaApi.goNext();
        } catch (error) {
            logError(error);
        }
    });

    repoNav.delegate("#repo-nav-ops-parent:not(.repo-nav-ops-disabled)", "click", function () {
        try {
            javaApi.goParent();
        } catch (error) {
            logError(error);
        }
    });

    repoNav.delegate("#repo-nav-ops-refresh:not(.repo-nav-ops-disabled)", "click", function () {
        loadRepoContent();
    });

    repoNav.delegate(".repo-nav-path-node", "click", function () {
        let pathString = $(this).attr("title");
        if (typeof (pathString) != "undefined") {
            goPath(pathString);
        }
    });

    $(window).resize(function () {
        adjustRepoNavPath();
    });

    repoContentTable.delegate("th-responsive", "click", function () {
        let sortKey = $(this).attr("sort-key");

    });

    $(document).keydown(function (event) {
        if (event.ctrlKey && event.keyCode === 65) {
            if ($("#sidebar-tab > .layui-this").attr("id") === "sidebar-repo-home") {
                $(".repo-content-check").prop("checked", true).change();
            }
        }
    });

    repoContentTable.delegate("#repo-content-check-all", "click", function (event) {
        event.stopPropagation();
        $(".repo-content-check").prop("checked", $(this).prop("checked")).change();
    });

    repoContentTable.delegate(".repo-content-check", "click", function (event) {
        event.stopPropagation();
        repoContentCheckAll.prop("checked", $(this).prop("checked") && allChecked());
    });

    repoContentTable.delegate(".repo-content-check", "change", function (event) {
        event.stopPropagation();
        let classSelected = "tr-selected";
        if ($(this).prop("checked")) {
            $(this).parents("tr").addClass(classSelected);
            repoOpsOnselect.show();
        } else {
            $(this).parents("tr").removeClass(classSelected);
            if (allNotChecked()) {
                repoOpsOnselect.hide();
            }
        }
        repoContentCheckAll.prop("checked", $(this).prop("checked") && allChecked());
    });

    repoContentTable.delegate("tbody > tr", "click", function (event) {
        event.stopPropagation();
        $(".repo-content-check").prop("checked", false).change();
        $(this).find(".repo-content-check").prop("checked", true).change();
    });

    repoContentTable.delegate("tbody > tr.repo-content-tr-DIR", "dblclick", function (event) {
        event.stopPropagation();
        $(this).find("td.repo-content-td-name > span").click();
    });

    repoContentTable.delegate("tbody > tr.repo-content-tr-DIR > td.repo-content-td-name > span", "click", function (event) {
        event.stopPropagation();
        openDir($(this).parent().attr("title"));
    });
});
