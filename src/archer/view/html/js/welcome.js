let rowHostPort = $("#row-host-port");
let rowAuthType = $("#row-authType");
let rowPrivateKeyPassphrase = $("#row-privateKey-passphrase");
let inputPort = rowHostPort.find("input[name=port]");
let authTypePassword = rowAuthType.find("input[name=authType][value=password]");
let authTypePrivateKey = rowAuthType.find("input[name=authType][value=privateKey]");
let formItemPassword = $("#form-add-repo-item-password");
let repoList = $("#repo-list");


function authWithPassword() {
    authTypePassword.prop("checked", true);
    formItemPassword.show();
    rowPrivateKeyPassphrase.hide();
}

function authWithPrivateKey() {
    authTypePrivateKey.prop("checked", true);
    formItemPassword.hide();
    rowPrivateKeyPassphrase.show();
}

function svnSshFormInit() {
    rowHostPort.show();
    inputPort.attr("placeholder", "22");
    rowAuthType.show();
    authWithPassword();
}

function svnFormInit() {
    rowHostPort.show();
    inputPort.attr("placeholder", "3690");
    rowAuthType.hide();
    authWithPassword();
}

function fileFormInit() {
    rowHostPort.hide();
    rowAuthType.hide();
    authWithPassword();
}

function loadRepoList() {
    try {
        let repoConfigList = javaApi.loadRepositoryConfigList();
        let sidebarRepoList = $("#sidebar-repo-list");
        if (repoConfigList.length > 0) {
            repoList.html(layui.laytpl($("#repo-list-item-tpl").html()).render(repoConfigList));
            sidebarRepoList.show();
        } else {
            repoList.empty();
            sidebarRepoList.hide();
            switchToSidebarTab("sidebar-add-repo");
        }
    } catch (error) {
        logError(error);
    }
}


layui.use(["element", "form", "laytpl"], function () {
    const element = layui.element;
    const form = layui.form;

    element.tab({
        headerElem: "#sidebar-tab>.sidebar-tab-item",
        bodyElem: "#content>.content-item",
    });

    element.on("tab(sidebar-tab)", function (data) {
        let sidebarTabId = data.elem.prevObject.attr("id");
        if (sidebarTabId === "sidebar-repo-list") {
            loadRepoList();
        }
    });

    form.verify({
        port: function (value) {
            if (value.length > 0) {
                if (!/^\d+$/.test(value) || parseInt(value) < 0 || parseInt(value) > 65535) {
                    return "端口不合法";
                }
            }
        }
    });

    form.on("select(form-add-repo-protocol)", function (data) {
        if (data.value === "svn+ssh") {
            svnSshFormInit();
        } else if (data.value === "svn") {
            svnFormInit();
        } else if (data.value === "file") {
            fileFormInit();
        }
        form.render();
    });

    form.on("radio(form-add-repo-authType)", function (data) {
        if (data.value === "password") {
            authWithPassword();
        } else if (data.value === "privateKey") {
            authWithPrivateKey();
        }
        form.render();
    });

    $("#form-add-repo-reset").click(function () {
        svnSshFormInit();
        form.render();
    });

    form.on("submit(form-add-repo-submit)", function (data) {
        try {
            javaApi.addRepository(data.field);
        } catch (error) {
            logError(error);
        }
        return false;
    });

    repoList.delegate(".repo-list-item", "click", function () {
        try {
            javaApi.openRepository($(this).index());
        } catch (error) {
            logError(error);
        }
    });

    repoList.delegate(".repo-list-item", "mouseenter", function () {
        $(".repo-list-item").removeClass("repo-list-item-selected");
        $(this).addClass("repo-list-item-selected");
        $(".repo-list-item-remove").hide();
        $(this).find(".repo-list-item-remove").show();
    });

    repoList.delegate(".repo-list-item-remove", "click", function (e) {
        e.stopPropagation();
        try {
            javaApi.removeRepository($(this).parents(".repo-list-item").index());
        } catch (error) {
            logError(error);
        }
    });
});
