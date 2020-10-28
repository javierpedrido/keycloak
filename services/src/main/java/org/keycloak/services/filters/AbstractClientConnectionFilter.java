package org.keycloak.services.filters;

/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.services.resources.KeycloakApplication;


public abstract class AbstractClientConnectionFilter {

    public void filter(ClientConnection clientConnection, Consumer<KeycloakSession> next) {
        KeycloakSessionFactory sessionFactory = KeycloakApplication.getSessionFactory();
        KeycloakSession session = sessionFactory.create();

        KeycloakTransactionManager tx = session.getTransactionManager();
        tx.begin();

        try {
            Resteasy.pushContext(ClientConnection.class, clientConnection);
            Resteasy.pushContext(KeycloakSession.class, session);

            next.accept(session);
        } catch (Exception e) {
            tx.setRollbackOnly();
            throw new RuntimeException(e);
        } finally {
            close(session);
        }
    }

    public void close(KeycloakSession session) {
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (tx.isActive()) {
            if (tx.getRollbackOnly()) {
                tx.rollback();
            } else {
                tx.commit();
            }
        }

        session.close();
    }
}