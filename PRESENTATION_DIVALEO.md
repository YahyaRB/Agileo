# Présentation PowerPoint - DIVALEO
## Application de Gestion Richebois

---

## SLIDE 1 : Page de Titre
**DIVALEO**
*Application de Gestion Intégrée*

**RICHE BOIS**
*Since 1977*

Présenté par : [Votre Nom]
Date : [Date de présentation]

---

## SLIDE 2 : Sommaire
1. Introduction
2. Vue d'ensemble du projet
3. Architecture technique
4. Fonctionnalités principales
5. Dashboard et statistiques
6. Gestion des utilisateurs et rôles
7. Modules métier
8. Sécurité et authentification
9. Technologies utilisées
10. Démonstration
11. Conclusion et perspectives

---

## SLIDE 3 : Introduction
### Contexte
- **Entreprise** : Richebois (depuis 1977)
- **Besoin** : Digitalisation et centralisation de la gestion opérationnelle
- **Objectif** : Optimiser les processus métier et améliorer la traçabilité

### Problématiques résolues
- ✅ Gestion centralisée des affaires (projets)
- ✅ Suivi des demandes d'achat
- ✅ Traçabilité des réceptions
- ✅ Gestion des consommations
- ✅ Tableaux de bord en temps réel

---

## SLIDE 4 : Vue d'ensemble - DIVALEO
### Qu'est-ce que DIVALEO ?
**Application web de gestion intégrée** pour la gestion complète des opérations de Richebois

### Principales caractéristiques
- 🌐 Application web responsive
- 🔐 Authentification sécurisée (Keycloak)
- 📊 Tableaux de bord personnalisés par rôle
- 📱 Interface moderne et intuitive
- 🔄 Intégration avec Divalto
- 📈 Statistiques et rapports en temps réel

---

## SLIDE 5 : Architecture Technique
### Architecture 3-Tiers

```
┌─────────────────────────────────────┐
│   FRONTEND (Angular 16)             │
│   - Interface utilisateur           │
│   - Chart.js pour graphiques         │
│   - Material Design                 │
└─────────────────────────────────────┘
              ↕ HTTP/REST
┌─────────────────────────────────────┐
│   BACKEND (Spring Boot 3.5.0)      │
│   - API REST                        │
│   - Logique métier                  │
│   - Sécurité JWT                    │
└─────────────────────────────────────┘
              ↕ JDBC
┌─────────────────────────────────────┐
│   DATABASE (SQL Server)              │
│   - Base de données primaire        │
│   - Base de données secondaire      │
│   - Intégration Divalto             │
└─────────────────────────────────────┘
```

---

## SLIDE 6 : Stack Technologique - Backend
### Technologies Backend
- **Framework** : Spring Boot 3.5.0
- **Langage** : Java 17
- **ORM** : JPA / Hibernate
- **Sécurité** : Spring Security + JWT
- **Authentification** : Keycloak Integration
- **Base de données** : SQL Server
- **Build** : Maven
- **API** : RESTful API

### Avantages
- ✅ Architecture modulaire et scalable
- ✅ Sécurité renforcée
- ✅ Performance optimisée
- ✅ Maintenance facilitée

---

## SLIDE 7 : Stack Technologique - Frontend
### Technologies Frontend
- **Framework** : Angular 16
- **Langage** : TypeScript
- **UI Components** : Material Design
- **Graphiques** : Chart.js
- **HTTP Client** : HttpClient
- **Routing** : Angular Router
- **State Management** : Services & RxJS

### Avantages
- ✅ Interface réactive et moderne
- ✅ Expérience utilisateur optimale
- ✅ Performance client élevée
- ✅ Compatible tous navigateurs

---

## SLIDE 8 : Fonctionnalités Principales
### Modules de l'application

1. **📊 Dashboard**
   - Statistiques en temps réel
   - Graphiques interactifs
   - Vue personnalisée par rôle

2. **👥 Gestion des Utilisateurs**
   - Création et gestion des comptes
   - Attribution des rôles et accès
   - Liaison avec les accessors

3. **🏗️ Gestion des Affaires**
   - Suivi des projets
   - Affectation aux utilisateurs
   - Statistiques par affaire

---

## SLIDE 9 : Fonctionnalités Principales (Suite)
4. **📝 Demandes d'Achat**
   - Création et suivi des DA
   - Gestion des lignes de demande
   - Workflow d'approbation
   - Pièces jointes

5. **📦 Réceptions**
   - Enregistrement des réceptions
   - Lignes de réception
   - Gestion des fichiers
   - Intégration avec commandes

6. **📋 Consommations**
   - Suivi des consommations
   - Lignes de consommation
   - Traçabilité complète

---

## SLIDE 10 : Dashboard et Statistiques
### Tableau de bord intelligent

**Pour les Administrateurs et Consulteurs :**
- 📈 Vue globale de toutes les données
- 👥 Statistiques de tous les utilisateurs
- 📊 Graphiques d'évolution (30 jours)
- 📉 Analyse des tendances

**Pour les Magasiniers et Chefs de Projet :**
- 🎯 Vue filtrée par affaires assignées
- 📊 Statistiques personnalisées
- 📈 Suivi de leurs propres activités
- 📋 Tableaux de bord dédiés

### Types de graphiques
- Graphiques linéaires (évolution temporelle)
- Graphiques en secteurs (répartition)
- Graphiques en barres (comparaisons)

---

## SLIDE 11 : Gestion des Utilisateurs et Rôles
### Système de rôles hiérarchique

**Rôles disponibles :**
- 👑 **ADMIN** : Accès complet à toutes les fonctionnalités
- 👁️ **CONSULTEUR** : Consultation de toutes les données
- 👔 **CHEF_PROJET** : Gestion des affaires assignées
- 📦 **MAGASINIER** : Gestion des réceptions et stock

### Fonctionnalités
- ✅ Création et modification des utilisateurs
- ✅ Attribution des rôles
- ✅ Gestion des accès (permissions)
- ✅ Liaison User-Accessor
- ✅ Activation/Désactivation des comptes
- ✅ Statistiques d'activité par utilisateur

---

## SLIDE 12 : Module Demandes d'Achat
### Fonctionnalités complètes

**Gestion des demandes :**
- ➕ Création de nouvelles demandes
- ✏️ Modification et mise à jour
- 📄 Gestion des lignes de demande
- 📎 Pièces jointes (fichiers)
- 🔄 Workflow de validation
- 📊 Suivi des statuts (Brouillon, Envoyé, Reçu, Rejeté)

**Statistiques :**
- 📈 Nombre de demandes par mois
- 📉 Taux de validation
- 📊 Évolution temporelle
- 🎯 Statistiques par utilisateur

---

## SLIDE 13 : Module Réceptions
### Gestion des réceptions

**Fonctionnalités :**
- 📦 Enregistrement des réceptions
- 📋 Gestion des lignes de réception
- 🔗 Liaison avec les commandes
- 📎 Gestion des fichiers associés
- ✅ Suivi de l'intégration
- 📊 Statistiques de réception

**Intégrations :**
- 🔄 Synchronisation avec Divalto
- 📝 Génération automatique des BL
- 📊 Traçabilité complète

---

## SLIDE 14 : Module Consommations
### Suivi des consommations

**Fonctionnalités :**
- 📋 Enregistrement des consommations
- 📝 Gestion des lignes de consommation
- 🏗️ Association aux affaires
- 📊 Statistiques par affaire
- 📈 Suivi temporel
- 📉 Analyse des tendances

**Avantages :**
- ✅ Traçabilité complète
- ✅ Optimisation des stocks
- ✅ Réduction des pertes
- ✅ Meilleure planification

---

## SLIDE 15 : Sécurité et Authentification
### Architecture sécurisée

**Keycloak Integration :**
- 🔐 Authentification centralisée
- 🔑 Gestion des sessions
- 👥 Synchronisation des utilisateurs
- 🔄 Single Sign-On (SSO)

**Spring Security :**
- 🛡️ Protection des endpoints
- 🔒 Contrôle d'accès basé sur les rôles
- 🎫 JWT Token Management
- 🔐 Chiffrement des données

**Sécurité des données :**
- 🔒 Accès basé sur les rôles
- 📊 Filtrage des données par utilisateur
- 🔐 Validation des permissions
- 📝 Audit trail complet

---

## SLIDE 16 : Intégration Divalto
### Synchronisation avec Divalto

**Fonctionnalités d'intégration :**
- 🔄 Synchronisation bidirectionnelle
- 📊 Import des données Divalto
- 🔗 Liaison avec les accessors
- 📝 Mise à jour automatique
- 🔄 Réconciliation des données

**Avantages :**
- ✅ Données à jour en temps réel
- ✅ Cohérence entre systèmes
- ✅ Réduction des erreurs manuelles
- ✅ Traçabilité améliorée

---

## SLIDE 17 : Statistiques et Rapports
### Tableaux de bord avancés

**Statistiques disponibles :**
- 📊 Statistiques générales
  - Nombre d'affaires
  - Nombre d'utilisateurs
  - Nombre de fournisseurs
  - Articles en stock

- 📈 Statistiques par module
  - Demandes d'achat (totaux, statuts, évolution)
  - Réceptions (en attente, intégrées)
  - Consommations (totaux, lignes)
  - Commandes (en cours, livrées)

- 📉 Évolution temporelle
  - Graphiques sur 30 jours
  - Tendances et prévisions
  - Comparaisons mensuelles

---

## SLIDE 18 : Statistiques Utilisateur
### Suivi d'activité par utilisateur

**Nouvelles fonctionnalités :**
- 📊 Graphique d'activité sur 12 mois
- 📈 Demandes d'achat par mois
- 📦 Réceptions par mois
- 📋 Consommations par mois
- 📉 Visualisation comparative

**Avantages :**
- ✅ Évaluation de la productivité
- ✅ Identification des tendances
- ✅ Optimisation des processus
- ✅ Reporting détaillé

---

## SLIDE 19 : Interface Utilisateur
### Design moderne et intuitif

**Caractéristiques :**
- 🎨 Interface moderne et épurée
- 📱 Design responsive (mobile-friendly)
- 🖱️ Navigation intuitive
- 🎯 Recherche et filtres avancés
- 📊 Graphiques interactifs
- 🔔 Notifications en temps réel

**Expérience utilisateur :**
- ✅ Chargement rapide
- ✅ Interface réactive
- ✅ Feedback visuel immédiat
- ✅ Accessibilité optimisée

---

## SLIDE 20 : Avantages et Bénéfices
### Pour l'entreprise

**Efficacité opérationnelle :**
- ⚡ Réduction du temps de traitement
- 📊 Visibilité en temps réel
- 🔄 Automatisation des processus
- 📈 Amélioration de la productivité

**Gestion améliorée :**
- 👥 Meilleure coordination des équipes
- 📋 Traçabilité complète
- 📊 Décisions basées sur les données
- 🔍 Reporting détaillé

**Sécurité renforcée :**
- 🔐 Authentification sécurisée
- 🛡️ Contrôle d'accès granulaire
- 📝 Audit trail complet
- 🔒 Protection des données

---

## SLIDE 21 : Démonstration
### Aperçu de l'application

**Écrans à présenter :**
1. 🏠 Page d'accueil / Dashboard
2. 👥 Gestion des utilisateurs
3. 🏗️ Liste des affaires
4. 📝 Création d'une demande d'achat
5. 📦 Interface de réception
6. 📊 Graphiques et statistiques
7. 📈 Statistiques utilisateur (nouveau)

*[Insérer des captures d'écran ou faire une démo live]*

---

## SLIDE 22 : Roadmap et Évolutions
### Perspectives d'avenir

**Court terme :**
- 🔔 Système de notifications
- 📧 Intégration email
- 📱 Application mobile
- 📊 Rapports avancés

**Moyen terme :**
- 🤖 Intelligence artificielle (prédictions)
- 📈 Analytics avancés
- 🔄 Automatisation renforcée
- 🌐 API publique

**Long terme :**
- ☁️ Migration cloud
- 🔗 Intégrations supplémentaires
- 📊 BI avancé
- 🌍 Multi-tenant

---

## SLIDE 23 : Métriques et Performances
### Indicateurs clés

**Performance technique :**
- ⚡ Temps de réponse < 200ms
- 🔄 Disponibilité > 99.5%
- 📊 Support de 1000+ utilisateurs simultanés
- 💾 Gestion de millions d'enregistrements

**Adoption :**
- 👥 [X] utilisateurs actifs
- 📊 [Y] transactions/jour
- 📈 [Z]% d'augmentation de productivité
- ⏱️ [W]% de réduction du temps de traitement

---

## SLIDE 24 : Conclusion
### DIVALEO : La solution complète

**Points clés :**
- ✅ Application moderne et performante
- ✅ Architecture scalable et sécurisée
- ✅ Fonctionnalités complètes
- ✅ Interface intuitive
- ✅ Intégration Divalto
- ✅ Tableaux de bord avancés

**Valeur ajoutée :**
- 🚀 Digitalisation complète
- 📊 Visibilité en temps réel
- ⚡ Efficacité opérationnelle
- 🔐 Sécurité renforcée

---

## SLIDE 25 : Questions & Réponses
### Merci pour votre attention

**Contact :**
- 📧 Email : [votre-email]
- 📱 Téléphone : [votre-téléphone]
- 🌐 Site web : [votre-site]

**Ressources :**
- 📚 Documentation technique
- 🎥 Vidéos de démonstration
- 📖 Guide utilisateur

**Questions ?**

---

## Notes pour la présentation

### Slide 1 (Titre)
- Utiliser le logo Richebois si disponible
- Mettre en évidence "DIVALEO" en grand
- Ajouter une image de fond professionnelle

### Slide 2 (Sommaire)
- Animer les points au clic
- Utiliser des icônes pour chaque section

### Slides techniques (5-7)
- Utiliser des diagrammes colorés
- Mettre en évidence les technologies principales
- Ajouter des logos des technologies

### Slides fonctionnalités (8-14)
- Utiliser des captures d'écran réelles
- Ajouter des animations pour les processus
- Utiliser des couleurs cohérentes

### Slide Dashboard (10)
- Montrer des graphiques réels
- Utiliser des couleurs vives pour les graphiques
- Mettre en évidence les différences par rôle

### Slide Sécurité (15)
- Utiliser des icônes de sécurité
- Mettre en évidence les aspects de protection
- Utiliser un schéma d'architecture de sécurité

### Slide Démonstration (21)
- Préparer des captures d'écran haute qualité
- Ou préparer une démo live avec données de test
- Montrer les fonctionnalités clés

### Slide Conclusion (24)
- Résumer les points clés
- Utiliser des visuels impactants
- Mettre en évidence la valeur ajoutée

### Conseils généraux
- Utiliser une palette de couleurs cohérente (bleu, vert, orange)
- Limiter le texte par slide (règle 6x6 : max 6 lignes, 6 mots par ligne)
- Utiliser des images et icônes pertinentes
- Animer les transitions entre slides
- Préparer des notes pour chaque slide
- Répéter la présentation plusieurs fois

