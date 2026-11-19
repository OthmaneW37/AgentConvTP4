# Utilisation de l'IA dans le TP 4 (application Web)

J'ai utilisé Claude pendant ce TP pour comprendre les problèmes où je suis resté bloqué.

Problèmes rencontrés :
- Je ne savais pas où faire l'ingestion des PDF pour qu'elle ne soit faite qu'une
  seule fois. Claude m'a expliqué qu'un bean @ApplicationScoped n'existe qu'en un
  seul exemplaire pour toute l'application ; j'y ai mis l'ingestion
  (MagasinEmbeddings) et je l'ai lancée au démarrage avec
  @Observes @Initialized(ApplicationScoped.class).
- Les PDF n'étaient pas trouvés dans Payara. Claude m'a aidé à comprendre que le
  chargeur de classes par défaut n'est pas celui de l'application déployée ; j'ai
  utilisé MagasinEmbeddings.class.getClassLoader() pour les charger.
- Je ne voyais pas comment combiner les tests 3 et 4. Claude m'a aidé à comprendre
  qu'un QueryRouter personnalisé peut déléguer à un autre ; j'ai écrit un QueryRouter
  qui demande d'abord au LM si la question porte sur un des 2 PDF, puis qui délègue
  au LanguageModelQueryRouter.
- Le formulaire ne s'envoyait pas. Claude m'a aidé à comprendre que la validation JSF
  bloquait l'envoi tant qu'aucun rôle n'était choisi dans la liste.
- Git voulait convertir les fins de ligne des fichiers PDF. Claude m'a expliqué que
  cela pouvait les abîmer, puisqu'un PDF est un fichier binaire ; j'ai ajouté un
  fichier .gitattributes avec "*.pdf binary".
- Le modèle gemini-2.5-flash renvoyait une erreur. Claude m'a aidé à comprendre que
  ce modèle n'était plus disponible ; LlmClient utilise gemini-3.5-flash-lite.