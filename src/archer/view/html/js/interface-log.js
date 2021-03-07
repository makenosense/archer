let logTreeOptions = {
    core: {
        themes: {
            name: "default-dark",
            dots: false,
            ellipsis: true,
        },
        animation: false,
    },
    types: {
        date: {icon: "date fas fa-history"},
        newDir: {
            icon: "fas fa-folder",
            li_attr: {class: "added"},
        },
        modifiedDir: {
            icon: "fas fa-folder",
            li_attr: {class: "modified"},
        },
        deletedDir: {
            icon: "fas fa-folder",
            li_attr: {class: "deleted"},
        },
        newFile: {
            icon: "far fa-file",
            li_attr: {class: "added"},
        },
        modifiedFile: {
            icon: "far fa-file",
            li_attr: {class: "modified"},
        },
        deletedFile: {
            icon: "far fa-file",
            li_attr: {class: "deleted"},
        },
    },
    plugins: ["types"],
};


function createLogTree(data) {
    logTreeOptions.core.data = data;
    destroyLogTree();
    $("#log-tree").jstree(logTreeOptions);
}

function destroyLogTree() {
    let logTree = $.jstree.reference("#log-tree");
    if (logTree != null) {
        logTree.destroy();
    }
}

$(function () {

});
