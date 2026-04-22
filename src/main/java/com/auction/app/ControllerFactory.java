package com.auction.app;

import com.auction.controller.DashboardController;
import com.auction.controller.LoginController;
import com.auction.controller.RegisterController;

public final class ControllerFactory {
    private ControllerFactory() {}

    public static Object create(Class<?> type, AppContext appContext) {
        if (type == LoginController.class) return new LoginController(appContext);
        if (type == RegisterController.class) return new RegisterController(appContext);
        if (type == DashboardController.class) return new DashboardController(appContext);
        throw new IllegalArgumentException("Không hỗ trợ controller: " + type.getName());
    }
}