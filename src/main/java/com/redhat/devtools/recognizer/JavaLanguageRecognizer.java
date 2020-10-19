/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JavaLanguageRecognizer {

    public static String getJava(List<String> files) throws ParserConfigurationException, SAXException, IOException {
        String result = "JAVA ";
        Optional<String> gradle = files.stream().filter(file -> FilenameUtils.getName(file).equalsIgnoreCase("build.gradle")).findFirst();
        Optional<String> maven = files.stream().filter(file -> FilenameUtils.getName(file).equalsIgnoreCase("pom.xml")).findFirst();
        Optional<String> ant = files.stream().filter(file -> FilenameUtils.getName(file).equalsIgnoreCase("build.xml")).findFirst();
        Path file = null;
        if (gradle.isPresent()) {
            file = Paths.get(gradle.get());
            result += "Gradle ";
        } else if (maven.isPresent()) {
            file = Paths.get(maven.get());
            result += "Maven ";
        } else if (ant.isPresent()) {
            result += "Ant ";
        }

        if (file != null) {
            boolean hasQuarkus = hasQuarkus(file, gradle.isPresent(), maven.isPresent());
            if (hasQuarkus) {
                result += "Quarkus ";
            }
            boolean hasSpring = hasSpringBoot(file, gradle.isPresent(), maven.isPresent());
            if (hasSpring) {
                result += "Spring ";
            }
            boolean hasOpenLiberty = hasOpenLiberty(file, gradle.isPresent(), maven.isPresent());
            if (hasOpenLiberty) {
                result += "OpenLiberty ";
            }
        }

        return result;
    }

    public static boolean hasOpenLiberty(Path file, boolean isGradle, boolean isMaven) throws IOException, ParserConfigurationException, SAXException {
        String openTag = "io.openliberty";
        if (isGradle) {
            return hasDependencyInGradle(file, openTag);
        } else if (isMaven) {
            return hasGroupIdMaven(file, openTag);
        }
        return false;
    }

    public static boolean hasSpringBoot(Path file, boolean isGradle, boolean isMaven) throws IOException, ParserConfigurationException, SAXException {
        String springTag = "org.springframework";
        if (isGradle) {
            return hasDependencyInGradle(file, springTag);
        } else if (isMaven) {
            return hasGroupIdMaven(file, springTag);
        }

        return false;
    }

    public static boolean hasQuarkus(Path file, boolean isGradle, boolean isMaven) throws IOException, ParserConfigurationException, SAXException {
        String quarkusTag = "io.quarkus";
        if (isGradle) {
            return hasDependencyInGradle(file, quarkusTag);
        } else if (isMaven) {
            return hasGroupIdMaven(file, quarkusTag);
        }

        return false;
    }

    private static boolean hasGroupIdMaven(Path file, String groupId) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file.toFile());
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("groupId");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getTextContent().startsWith(groupId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDependencyInGradle(Path file, String dependency) throws IOException {
        return Files.readAllLines(file).stream().anyMatch(line -> line.contains(dependency));
    }
}
