import { createCatalogDemoApp } from "./useCatalogDemo.js";

const app = Vue.createApp(createCatalogDemoApp());

["Plus", "Refresh", "Folder", "Edit", "Delete", "Right", "FolderOpened"]
    .forEach((name) => {
        app.component(name, ElementPlusIconsVue[name]);
    });

app.use(ElementPlus);
app.mount("#app");
