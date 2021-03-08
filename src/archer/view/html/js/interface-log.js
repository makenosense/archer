let logTreeOptions = {
    core: {
        themes: {
            name: "default-dark",
            dots: false,
        },
        animation: false,
        dblclick_toggle: false,
    },
    types: {
        date: {icon: "fas fa-calendar-alt", li_attr: {class: "date"}},
        revision: {icon: "fas fa-history", li_attr: {class: "revision"}},
        dir: {icon: "fas fa-folder"},
        dir_a: {icon: "fas fa-folder", li_attr: {class: "added"}},
        dir_m: {icon: "fas fa-folder", li_attr: {class: "modified"}},
        dir_d: {icon: "fas fa-folder", li_attr: {class: "deleted"}},
        file_a: {icon: "far fa-file", li_attr: {class: "added"}},
        file_m: {icon: "far fa-file", li_attr: {class: "modified"}},
        file_d: {icon: "far fa-file", li_attr: {class: "deleted"}},
    },
    sort: function (n1, n2) {
        let type1 = this.get_type(n1), type2 = this.get_type(n2);
        let text1 = this.get_text(n1), text2 = this.get_text(n2);
        if ((type1 === "date" && type2 === "date")
            || (type1 === "revision" && type2 === "revision")) {
            return n1 <= n2 ? 1 : -1;
        } else {
            return text1 <= text2 ? -1 : 1;
        }
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
        $("#log-tree").jstree(logTreeOptions).on("select_node.jstree", function (e, data) {
            let logTree = $.jstree.reference("#log-tree");
            logTree.deselect_node(data.node);
            if (data.node.children.length > 0) {
                logTree.toggle_node(data.node);
            }
        });
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
});
