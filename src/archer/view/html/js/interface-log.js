let logTreeOptions = {
    core: {
        themes: {
            name: "default-dark",
            dots: false,
            ellipsis: true,
        },
        animation: false,
        dblclick_toggle: false,
    },
    types: {
        date: {icon: "fas fa-calendar-alt", li_attr: {class: "date"}},
        revision: {icon: "fas fa-history", li_attr: {class: "revision"}},
        dir_a: {icon: "fas fa-folder", li_attr: {class: "added"}},
        dir_m: {icon: "fas fa-folder", li_attr: {class: "modified"}},
        dir_d: {icon: "fas fa-folder", li_attr: {class: "deleted"}},
        file_a: {icon: "far fa-file", li_attr: {class: "added"}},
        file_m: {icon: "far fa-file", li_attr: {class: "modified"}},
        file_d: {icon: "far fa-file", li_attr: {class: "deleted"}},
    },
    plugins: ["types", "sort"],
};


function loadRepoLog() {
    try {
        javaApi.loadRepositoryLog();
    } catch (error) {
        logError(error);
    }
}

function setLogCacheRefreshingTime(lastRefreshingTime) {
    $("#log-last-refreshing-time").text(lastRefreshingTime);
}

function createLogTree() {
    try {
        let data = [];
        $.each(javaApi.getLogTreeNodeArray(), function (idx, treeNode) {
            data.push({
                id: treeNode.id,
                parent: treeNode.parent,
                type: treeNode.type,
                text: treeNode.text,
                state: {
                    opened: treeNode.state.opened,
                },
            });
        });
        logTreeOptions.core.data = data;
        destroyLogTree();
        $("#log-tree").jstree(logTreeOptions);
    } catch (error) {
        logError(error);
    }
}

function destroyLogTree() {
    let logTree = $.jstree.reference("#log-tree");
    if (logTree != null) {
        logTree.destroy();
    }
}

function expandAllLogTreeNodes() {
    let logTree = $.jstree.reference("#log-tree");
    if (logTree != null) {
        logTree.open_all();
    }
}

function collapseAllLogTreeNodes() {
    let logTree = $.jstree.reference("#log-tree");
    if (logTree != null) {
        logTree.close_all();
    }
}

$(function () {
    $('#log-tree').on('select_node.jstree', function (e, data) {
        let logTree = $.jstree.reference("#log-tree");
        logTree.deselect_node(data.node);
        if (data.node.children.length > 0) {
            logTree.toggle_node(data.node);
        }
    });
});
