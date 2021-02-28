let body = $("body");
let repoOpsOnselect = $("#repo-ops-onselect");
let repoNav = $("#repo-nav");
let repoNavPath = $("#repo-nav-path");
let repoContentTable = $("#repo-content-table");
let repoContentCheckAll = $("#repo-content-check-all");
let repoContentTableBody = $("#repo-content-table > tbody");
let statusCount = $("#repo-content-status-count > span");
let colResizeData = {};
let colResizeStage = null;


function repoContentLoading(dataReloading = true) {
    repoOpsOnselect.hide();
    repoContentCheckAll.prop("checked", false);
    if (dataReloading) {
        switchRepoNavOps("repo-nav-ops-refresh", false);
        statusCount.html($("#repo-content-status-count-loading-tpl").html());
        repoContentTableBody.html($("#repo-content-table-loading-tpl").html());
    }
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

function repoNavPathNodeWidthTotal() {
    let sum = 0;
    repoNavPath.children().each(function () {
        sum += $(this).outerWidth(true);
    });
    return sum;
}

function adjustRepoNavPath() {
    try {
        let pathNodeList = javaApi.getPathNodeArray();
        repoNavPath.html(layui.laytpl($("#repo-nav-path-node-tpl").html()).render(pathNodeList));
        let pathNodeEllipsis = $("#repo-nav-path-node-ellipsis");
        pathNodeEllipsis.hide();
        while (repoNavPath.width() < repoNavPathNodeWidthTotal()) {
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

function updateRepoNav() {
    try {
        switchRepoNavOps("repo-nav-ops-previous", javaApi.hasPrevious());
        switchRepoNavOps("repo-nav-ops-next", javaApi.hasNext());
        switchRepoNavOps("repo-nav-ops-parent", javaApi.hasParent());
        adjustRepoNavPath();
    } catch (error) {
        logError(error);
    }
}

function fillRepoContentTable() {
    try {
        let entryList = javaApi.getEntryArray();
        statusCount.html(entryList.length);
        if (entryList.length > 0) {
            repoContentTableBody.html(layui.laytpl($("#repo-content-table-tr-tpl").html()).render(entryList));
            resizeRepoContentTableColumn();
        } else {
            repoContentTableBody.html($("#repo-content-table-empty-tpl").html());
        }
    } catch (error) {
        logError(error);
    }
}

function showSortIcon(sortKey, direction) {
    $(".sort-icon").hide();
    $(".th-responsive[col-key='" + sortKey + "']").find(".sort-icon." + direction).show();
}

function sortEntryList(sortKey, direction) {
    let currentSortIcon = $(".sort-icon:visible");
    if (typeof (sortKey) == "undefined") {
        sortKey = "name";
        if (currentSortIcon.length > 0) {
            sortKey = currentSortIcon.parents(".th-responsive").attr("col-key");
        }
    }
    if (typeof (direction) == "undefined") {
        direction = "up";
        if (currentSortIcon.length > 0) {
            direction = currentSortIcon.hasClass("up") ? "up" : "down";
        }
    }
    try {
        repoContentLoading(false);
        javaApi.sortEntryList(sortKey, direction);
    } catch (error) {
        logError(error);
    }
}

function resizeRepoContentTableColumn() {
    $(".th-responsive").each(function () {
        let colKey = $(this).attr("col-key");
        let setWidth = parseInt($(this).css("width"));
        let columnItems = $("th[col-key='" + colKey + "'], td[col-key='" + colKey + "']");
        if (columnItems.length > 0) {
            columnItems.css("max-width", setWidth);
            columnItems.css("width", setWidth);
        }
    });
}

function allEntryChecked() {
    let result = true;
    $(".repo-content-check").each(function () {
        if (!$(this).prop("checked")) {
            result = false;
        }
    });
    return result;
}

function noExistingEntryChecked() {
    let result = true;
    $(".repo-content-check").not("#tr-new-dir .repo-content-check").each(function () {
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
    let trNewDirSelector = "#tr-new-dir";

    $("#repo-ops-create-dir").click(function () {
        let trNewDir = $(trNewDirSelector);
        if (!trNewDir.length) {
            repoContentTableBody.prepend($("#repo-content-table-tr-new-dir-tpl").html());
            trNewDir = $(trNewDirSelector);
        }
        trNewDir.click();
    });

    repoContentTable.delegate("#tr-new-dir", "click", function (event) {
        event.stopPropagation();
        $("#new-dir-name").focus();
    });

    body.keydown(function (e) {
        if (e.which === 27) {
            $(trNewDirSelector).remove();
        }
    });
});

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
});

$(function () {
    repoContentTable.find("th").mousemove(function (e) {
        let oLeft = $(this).offset().left
            , pLeft = e.clientX - oLeft;
        if (!colResizeData.inResizing) {
            let selfResizable = $(this).hasClass("th-responsive");
            let prevResizable = $(this).prev().hasClass("th-responsive");
            let inSelfResizeRegion = selfResizable && $(this).innerWidth() - pLeft <= 10;
            let inPrevResizeRegion = prevResizable && pLeft <= 10;
            colResizeData.resizeReady = inSelfResizeRegion || inPrevResizeRegion;
            colResizeData.resizeTh = colResizeData.resizeReady && inSelfResizeRegion ? $(this) : $(this).prev();
            body.css("cursor", (colResizeData.resizeReady ? "col-resize" : ""));
        }
    }).mouseleave(function () {
        if (!colResizeData.inResizing) {
            body.css("cursor", "");
        }
    }).mousedown(function (e) {
        if (colResizeData.resizeReady) {
            let thWidthTotal = function () {
                let sum = 0;
                repoContentTable.find("thead > tr > th").each(function () {
                    sum += $(this).outerWidth(true);
                });
                return sum;
            };
            let colKey = colResizeData.resizeTh.attr("col-key");
            e.preventDefault();
            colResizeData.inResizing = true;
            colResizeData.initOffset = {x: e.clientX, y: e.clientY};
            colResizeData.resizingItems = $("th[col-key='" + colKey + "'], td[col-key='" + colKey + "']");
            colResizeData.minWidth = parseInt(colResizeData.resizeTh.css("min-width"));
            colResizeData.initWidth = parseInt(colResizeData.resizeTh.css("width"));
            colResizeData.initWidthTotal = thWidthTotal();
        }
    });

    let endResizing = function () {
        colResizeData = {};
        body.css("cursor", "");
    };
    body.mousemove(function (e) {
        if (colResizeData.inResizing) {
            if (e.which > 0) {
                e.preventDefault();
                if (colResizeData.resizingItems.length > 0) {
                    let theadWidth = repoContentTable.find("thead").innerWidth();
                    let widthDelta = Math.min(e.clientX - colResizeData.initOffset.x,
                        theadWidth - colResizeData.initWidthTotal);
                    let setWidth = Math.max(colResizeData.initWidth + widthDelta,
                        colResizeData.minWidth);
                    colResizeData.resizingItems.css("max-width", setWidth);
                    colResizeData.resizingItems.css("width", setWidth);
                }
                colResizeStage = 1;
            } else {
                endResizing();
            }
        }
    }).mouseup(function (e) {
        if (colResizeData.inResizing) {
            e.preventDefault();
            endResizing();
        }
        if (colResizeStage === 2) {
            colResizeStage = null;
        }
    });

    $(window).resize(function () {
        resizeRepoContentTableColumn();
    });

    repoContentTable.delegate(".th-responsive", "click", function () {
        if (colResizeStage === 1) {
            colResizeStage = 2;
        } else {
            let sortKey = $(this).attr("col-key");
            let downDefaultKeys = ["mtime", "size"];
            let direction = downDefaultKeys.indexOf(sortKey) < 0 ? "up" : "down";
            let currentSortIcon = $(this).find(".sort-icon:visible");
            if (currentSortIcon.length > 0) {
                direction = currentSortIcon.hasClass("up") ? "down" : "up";
            }
            sortEntryList(sortKey, direction);
        }
    });

    body.keydown(function (event) {
        if (event.ctrlKey && event.which === 65) {
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
        repoContentCheckAll.prop("checked", allEntryChecked());
    });

    repoContentTable.delegate(".repo-content-check", "change", function (event) {
        event.stopPropagation();
        let classSelected = "tr-selected";
        if ($(this).prop("checked")) {
            $(this).parents("tr").addClass(classSelected);
            if ($(this).parents("tr").attr("id") !== "tr-new-dir") {
                repoOpsOnselect.show();
            }
        } else {
            $(this).parents("tr").removeClass(classSelected);
            if (noExistingEntryChecked()) {
                repoOpsOnselect.hide();
            }
        }
        repoContentCheckAll.prop("checked", allEntryChecked());
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
