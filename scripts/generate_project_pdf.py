from __future__ import annotations

import datetime as dt
import re
import textwrap
from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = PROJECT_ROOT / "src" / "main" / "java"
RESOURCES_ROOT = PROJECT_ROOT / "src" / "main" / "resources"
OUTPUT_PDF = PROJECT_ROOT / "Documentation_Projet_Guser.pdf"
OUTPUT_TXT = PROJECT_ROOT / "Documentation_Projet_Guser.txt"

PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;")
CLASS_RE = re.compile(r"\b(class|interface|record)\s+(\w+)")
IMPORT_RE = re.compile(r"^\s*import\s+([\w.*]+)\s*;")
METHOD_RE = re.compile(
    r"^\s*(public|protected|private)\s+"
    r"(?:static\s+|final\s+|synchronized\s+|native\s+|abstract\s+|strictfp\s+)*"
    r"(?:<[^>]+>\s+)?(.+?)\s+(\w+)\s*"
    r"\(([^)]*)\)\s*(?:throws\s+[^{]+)?\s*\{\s*$"
)
CTOR_RE = re.compile(
    r"^\s*(public|protected|private)\s+(\w+)\s*"
    r"\(([^)]*)\)\s*(?:throws\s+[^{]+)?\s*\{\s*$"
)


@dataclass
class JavaMethod:
    access: str
    name: str
    params: str
    return_type: str
    signature: str
    body: str
    is_constructor: bool


@dataclass
class JavaClass:
    package: str
    name: str
    relative_path: str
    imports: list[str]
    methods: list[JavaMethod]


CLASS_SUMMARIES = {
    "MainApp": "Point d'entree JavaFX. Charge la vue de connexion, applique le style global et gere un mode de secours si le chargement echoue.",
    "DatabaseConfig": "Centralise les parametres de connexion MySQL (URL, utilisateur, mot de passe) via des accesseurs statiques.",
    "DatabaseConnection": "Expose une fabrique de connexion JDBC en s'appuyant sur DatabaseConfig.",
    "MailConfig": "Centralise la configuration SMTP utilisee par EmailService.",
    "DashboardController": "Controleur JavaFX du tableau de bord admin (metriques, navigation vers les utilisateurs, deconnexion).",
    "LoginController": "Controleur JavaFX de l'authentification : captcha, mot de passe, 2FA email, oubli de mot de passe et login FaceID.",
    "UserController": "Controleur JavaFX de gestion des utilisateurs : CRUD, recherche, tri, validation des champs et enrollement FaceID.",
    "UserDao": "Couche d'acces aux donnees utilisateurs (CRUD, auth, verifications, evolution automatique du schema).",
    "User": "Modele metier utilisateur (etat, constructeurs, getters/setters et helpers).",
    "CaptchaApiService": "Genere un code captcha et tente de recuperer son image depuis un service HTTP externe.",
    "EmailService": "Construit et envoie des emails HTML brandes (verification, reset, 2FA, alerte de securite + piece jointe).",
    "FaceIdService": "Capture webcam et calcule un template biometrie (LBP), puis compare des templates par similarite cosinus.",
    "SceneManager": "Utilitaire de navigation JavaFX pour changer de vue/FXML et reappliquer la feuille CSS.",
    "SessionManager": "Stockage en memoire de l'utilisateur connecte pour la session applicative courante.",
}

METHOD_SUMMARIES = {
    "MainApp.start": "Initialise la fenetre principale, charge login-view.fxml et applique app.css; bascule sur une vue d'erreur si un probleme survient.",
    "MainApp.buildErrorMessage": "Construit une chaine d'erreur lisible en parcourant la chaine des causes de l'exception.",
    "MainApp.main": "Lance le cycle de vie JavaFX via Application.launch.",
    "DashboardController.initialize": "Alimente l'entete avec l'utilisateur de session puis recharge les metriques du dashboard.",
    "DashboardController.refreshMetrics": "Calcule le total des utilisateurs via UserDao et met a jour les labels d'indicateurs.",
    "DashboardController.ouvrirUtilisateurs": "Ouvre la vue de gestion des utilisateurs.",
    "DashboardController.deconnexion": "Vide la session en memoire puis renvoie vers l'ecran de connexion.",
    "LoginController.seConnecter": "Flux principal d'authentification admin: validations de saisie, captcha, mot de passe, role ADMIN, puis 2FA email.",
    "LoginController.seConnecterParVisage": "Authentifie un admin par comparaison FaceID entre template capture et templates en base.",
    "LoginController.motDePasseOublie": "Declenche la recuperation par email (code OTP) puis met a jour le mot de passe en base.",
    "LoginController.validateTwoFactor": "Genere et envoie un code 2FA temporaire, puis delegate la verification a une boite de dialogue.",
    "LoginController.handleFailedPasswordAttempt": "Compte les echecs de connexion par email et declenche une capture camera au 3e echec.",
    "LoginController.sendCaptureToUser": "Capture une image webcam et l'envoie en piece jointe d'un email d'alerte de securite.",
    "UserController.initialize": "Controle l'acces ADMIN, initialise la liste des roles et les listeners UI, puis charge les utilisateurs.",
    "UserController.ajouterUtilisateur": "Valide les champs, envoie un code de verification email, puis insere l'utilisateur si le code est confirme.",
    "UserController.modifierUtilisateur": "Met a jour l'utilisateur selectionne apres validation metier et persiste les changements en base.",
    "UserController.supprimerUtilisateur": "Supprime l'utilisateur selectionne apres confirmation explicite.",
    "UserController.renderUserCards": "Reconstruit la grille des cartes selon recherche et ordre de tri.",
    "UserController.enregistrerFaceId": "Capture un template FaceID et le lie a l'utilisateur ADMIN selectionne.",
    "UserController.validateFields": "Applique les regles metier sur nom/prenoms/email/mot de passe et unicite email.",
    "UserDao.ensureSchema": "Cree/ajuste automatiquement la table utilisateurs pour maintenir la compatibilite du schema.",
    "UserDao.findAll": "Retourne tous les utilisateurs tries du plus recent au plus ancien.",
    "UserDao.authenticate": "Verifie le couple email/mot_de_passe et retourne eventuellement l'utilisateur.",
    "UserDao.findAdminsWithFaceTemplate": "Retourne les comptes ADMIN qui possedent deja un template FaceID exploitable.",
    "EmailService.sendBrandedEmail": "Assemble un email HTML complet (logo inline, contenu, eventuelle piece jointe) puis l'envoie via SMTP.",
    "EmailService.resolveRecipientName": "Determine un nom destinataire via base de donnees, fallback fourni, ou derive de l'email.",
    "FaceIdService.captureTemplate": "Pipeline biometrie complet: capture webcam, recadrage, grayscale, histogramme LBP et encodage Base64.",
    "FaceIdService.compareTemplates": "Calcule une similarite cosinus entre deux templates FaceID pour produire un score [0..1].",
}


def parse_java_file(path: Path) -> JavaClass | None:
    content = path.read_text(encoding="utf-8")
    lines = content.splitlines()

    package = "default"
    imports: list[str] = []
    class_name = None

    for line in lines:
        package_match = PACKAGE_RE.match(line)
        if package_match:
            package = package_match.group(1)
        import_match = IMPORT_RE.match(line)
        if import_match:
            imports.append(import_match.group(1))
        if class_name is None:
            class_match = CLASS_RE.search(line)
            if class_match:
                class_name = class_match.group(2)

    if class_name is None:
        return None

    methods: list[JavaMethod] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if not re.match(r"^\s*(public|protected|private)\b", line):
            i += 1
            continue
        if "(" not in line:
            i += 1
            continue

        signature_lines = [line.strip()]
        j = i
        invalid_candidate = False
        while True:
            joined = " ".join(signature_lines)
            if "{" in joined:
                break
            if ";" in joined:
                invalid_candidate = True
                break
            if j + 1 >= len(lines):
                break
            j += 1
            signature_lines.append(lines[j].strip())
            if len(signature_lines) > 20:
                break

        if invalid_candidate:
            i += 1
            continue

        signature_raw = " ".join(signature_lines)
        signature = re.sub(r"\s+", " ", signature_raw).strip()
        if not signature.endswith("{"):
            i += 1
            continue
        if " class " in f" {signature} " or " interface " in f" {signature} " or " enum " in f" {signature} ":
            i += 1
            continue

        method_match = METHOD_RE.match(signature)
        ctor_match = CTOR_RE.match(signature)

        access = ""
        return_type = ""
        method_name = ""
        params = ""
        is_constructor = False

        if method_match:
            access = method_match.group(1)
            return_type = method_match.group(2).strip()
            method_name = method_match.group(3).strip()
            params = method_match.group(4).strip()
            if return_type == "record":
                i += 1
                continue
        elif ctor_match and ctor_match.group(2) == class_name:
            access = ctor_match.group(1)
            method_name = ctor_match.group(2).strip()
            params = ctor_match.group(3).strip()
            return_type = class_name
            is_constructor = True
        else:
            i += 1
            continue

        brace_balance = signature.count("{") - signature.count("}")
        body_lines: list[str] = []
        k = j + 1
        while k < len(lines) and brace_balance > 0:
            body_line = lines[k]
            body_lines.append(body_line)
            brace_balance += body_line.count("{") - body_line.count("}")
            k += 1

        body = "\n".join(body_lines)
        methods.append(
            JavaMethod(
                access=access,
                name=method_name,
                params=params,
                return_type=return_type,
                signature=signature.rstrip("{").strip(),
                body=body,
                is_constructor=is_constructor,
            )
        )
        i = max(i + 1, k)

    return JavaClass(
        package=package,
        name=class_name,
        relative_path=str(path.relative_to(PROJECT_ROOT)).replace("\\", "/"),
        imports=imports,
        methods=methods,
    )


def collect_classes() -> list[JavaClass]:
    classes: list[JavaClass] = []
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        parsed = parse_java_file(path)
        if parsed is not None:
            classes.append(parsed)
    return classes


def method_params_text(params: str) -> str:
    cleaned = params.strip()
    if not cleaned:
        return "Aucun parametre."
    parts = [part.strip() for part in cleaned.split(",") if part.strip()]
    return ", ".join(parts) if parts else "Aucun parametre."


def return_text(method: JavaMethod) -> str:
    if method.is_constructor:
        return "Constructeur (initialise une instance)."
    if method.return_type.strip().lower() == "void":
        return "Aucune valeur retour (effet de bord)."
    return f"Retourne: {method.return_type}."


def split_camel(name: str) -> str:
    return re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", name).lower()


def infer_method_summary(class_name: str, method: JavaMethod) -> str:
    custom = METHOD_SUMMARIES.get(f"{class_name}.{method.name}")
    if custom:
        return custom

    if method.is_constructor:
        return "Construit l'objet et positionne les attributs initiaux necessaires a son fonctionnement."

    lower_name = method.name.lower()
    body = method.body.lower()

    if method.name.startswith("get") and not method.params:
        field = split_camel(method.name[3:])
        return f"Expose la valeur courante de '{field}' sans modifier l'etat interne."

    if method.name.startswith("set") and method.params:
        field = split_camel(method.name[3:])
        return f"Met a jour l'attribut '{field}' avec la valeur fournie en parametre."

    if method.name.startswith("has") and not method.params:
        return "Effectue une verification booleenne sur l'etat de l'objet."

    actions: list[str] = []
    if "switchscene" in body:
        actions.append("navigation entre vues JavaFX")
    if "executequery" in body or "select " in body:
        actions.append("lecture SQL")
    if "executeupdate" in body or "insert " in body or "update " in body or "delete " in body:
        actions.append("ecriture SQL")
    if "transport.send" in body or "mime" in body or "smtp" in body:
        actions.append("envoi d'email")
    if "webcam" in body or "capture" in lower_name:
        actions.append("interaction camera")
    if "pattern" in body or "matches(" in body:
        actions.append("validation de format")
    if "random" in body or "nextint" in body:
        actions.append("generation pseudo-aleatoire")
    if "optional" in method.return_type.lower():
        actions.append("retour optionnel")
    if "alert" in body:
        actions.append("feedback utilisateur")
    if "base64" in body:
        actions.append("encodage/decodage binaire")

    if actions:
        details = ", ".join(actions[:4])
        return f"Gere la logique metier de '{split_camel(method.name)}' avec les operations suivantes: {details}."

    return f"Implante la logique de '{split_camel(method.name)}' selon le contexte de la classe {class_name}."


def infer_method_risks(method: JavaMethod) -> str:
    body = method.body.lower()
    risks: list[str] = []
    if "sql" in body or "executequery" in body or "executeupdate" in body:
        risks.append("depend de la disponibilite MySQL")
    if "messagingexception" in body or "transport.send" in body:
        risks.append("peut echouer si SMTP est mal configure")
    if "webcam" in body:
        risks.append("necessite une camera accessible")
    if "uri.create" in body or "openstream" in body:
        risks.append("depend d'un service HTTP externe")
    if "showandwait" in body:
        risks.append("bloque l'UI pendant l'attente utilisateur")
    if "new random" in body or "random " in body:
        risks.append("code pseudo-aleatoire non cryptographique")
    if not risks:
        return "Risque faible; methode locale sans dependance externe critique."
    return "; ".join(risks) + "."


def project_tree_lines() -> list[str]:
    wanted = [
        "pom.xml",
        "src/main/java/com/guser/MainApp.java",
        "src/main/java/com/guser/config/DatabaseConfig.java",
        "src/main/java/com/guser/config/DatabaseConnection.java",
        "src/main/java/com/guser/config/MailConfig.java",
        "src/main/java/com/guser/controller/DashboardController.java",
        "src/main/java/com/guser/controller/LoginController.java",
        "src/main/java/com/guser/controller/UserController.java",
        "src/main/java/com/guser/dao/UserDao.java",
        "src/main/java/com/guser/model/User.java",
        "src/main/java/com/guser/service/CaptchaApiService.java",
        "src/main/java/com/guser/service/EmailService.java",
        "src/main/java/com/guser/service/FaceIdService.java",
        "src/main/java/com/guser/util/SceneManager.java",
        "src/main/java/com/guser/util/SessionManager.java",
        "src/main/resources/database.sql",
        "src/main/resources/com/guser/login-view.fxml",
        "src/main/resources/com/guser/user-view.fxml",
        "src/main/resources/com/guser/dashboard-view.fxml",
        "src/main/resources/com/guser/app.css",
        "src/main/resources/com/guser/assets/logo-otemps.png",
        "src/main/resources/com/guser/assets/logo.png",
    ]

    lines = ["Guser/", "  .mvn/", "  .idea/", "  src/", "    main/", "      java/", "        com/guser/"]
    for path in wanted[1:15]:
        lines.append(f"          {path.split('com/guser/')[1]}")
    lines.extend(
        [
            "      resources/",
            "        database.sql",
            "        com/guser/",
            "          login-view.fxml",
            "          user-view.fxml",
            "          dashboard-view.fxml",
            "          app.css",
            "          assets/",
            "            logo-otemps.png",
            "            logo.png",
            "  pom.xml",
            "  target/ (artefacts Maven compiles)",
        ]
    )
    return lines


def build_document_lines(classes: list[JavaClass]) -> list[str]:
    now = dt.datetime.now().strftime("%Y-%m-%d %H:%M")
    total_methods = sum(len(c.methods) for c in classes)

    lines: list[str] = []
    lines.append("DOCUMENTATION COMPLETE - PROJET GUSER")
    lines.append(f"Genere automatiquement le {now}")
    lines.append("")
    lines.append("1. OBJECTIF DU PROJET")
    lines.append(
        "Application JavaFX d'administration d'utilisateurs avec authentification admin renforcee (CAPTCHA, 2FA email, FaceID), gestion CRUD et alertes de securite."
    )
    lines.append("")
    lines.append("2. STACK TECHNIQUE")
    lines.append("- Java 17 + JavaFX 17")
    lines.append("- Maven (build + execution JavaFX)")
    lines.append("- MySQL via JDBC (mysql-connector-j)")
    lines.append("- Jakarta Mail (emails OTP/alertes)")
    lines.append("- Webcam Capture (capture image/FaceID)")
    lines.append("")
    lines.append("3. STRUCTURE DU PROJET")
    lines.extend(project_tree_lines())
    lines.append("")
    lines.append("4. INVENTAIRE GLOBAL DES CLASSES ET METHODES")
    for cls in classes:
        lines.append(f"- {cls.package}.{cls.name}: {len(cls.methods)} methodes/constructeurs")
    lines.append(f"TOTAL METHODES/CONSTRUCTEURS DETECTES: {total_methods}")
    lines.append("")
    lines.append("5. DETAIL COMPLET PAR CLASSE")
    lines.append("")

    for cls in classes:
        lines.append("=" * 95)
        lines.append(f"Classe: {cls.package}.{cls.name}")
        lines.append(f"Fichier: {cls.relative_path}")
        lines.append(f"Responsabilite: {CLASS_SUMMARIES.get(cls.name, 'Responsabilite metier specifique a cette classe.')}")
        if cls.imports:
            important_imports = ", ".join(cls.imports[:8])
            lines.append(f"Dependances principales: {important_imports}")
        else:
            lines.append("Dependances principales: Aucune importation explicite.")
        lines.append(f"Nombre de methodes/constructeurs: {len(cls.methods)}")
        lines.append("")
        if not cls.methods:
            lines.append("Aucune methode avec modificateur d'acces detectee.")
            lines.append("")
            continue

        for index, method in enumerate(cls.methods, start=1):
            lines.append(f"{index}. Signature: {method.signature}")
            lines.append(f"   Role: {infer_method_summary(cls.name, method)}")
            lines.append(f"   Parametres: {method_params_text(method.params)}")
            lines.append(f"   Retour: {return_text(method)}")
            lines.append(f"   Risques/contraintes: {infer_method_risks(method)}")
            lines.append("")

    lines.append("=" * 95)
    lines.append("6. OBSERVATIONS TECHNIQUES IMPORTANTES")
    lines.append("- Les mots de passe utilisateurs sont compares/stockes en clair dans la logique actuelle (pas de hachage).")
    lines.append("- Les identifiants SMTP et JDBC sont codes en dur dans les classes de configuration.")
    lines.append("- FaceID est une implementation locale LBP/cosinus: utile pour prototype, a durcir pour production.")
    lines.append("- UserDao applique une migration automatique de schema au demarrage (avantage: resiliente; risque: operation non tracee).")
    lines.append("")
    lines.append("FIN DU DOCUMENT")
    return lines


def wrap_lines(lines: list[str], width: int = 98) -> list[str]:
    wrapped: list[str] = []
    for line in lines:
        if not line.strip():
            wrapped.append("")
            continue
        if line.startswith("  ") or line.startswith("="):
            wrapped.append(line)
            continue
        for part in textwrap.wrap(
            line,
            width=width,
            replace_whitespace=False,
            drop_whitespace=False,
            break_long_words=False,
            break_on_hyphens=False,
        ):
            wrapped.append(part.rstrip())
    return wrapped


def paginate(lines: list[str], page_size: int = 52) -> list[list[str]]:
    pages: list[list[str]] = []
    for i in range(0, len(lines), page_size):
        pages.append(lines[i : i + page_size])
    return pages


def pdf_escape(text: str) -> str:
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def build_pdf_bytes(pages: list[list[str]]) -> bytes:
    objects: list[bytes] = []

    # 1: catalog, 2: pages, 3: font
    objects.append(b"<< /Type /Catalog /Pages 2 0 R >>")

    kids = []
    for page_index in range(len(pages)):
        page_obj_id = 4 + page_index * 2
        kids.append(f"{page_obj_id} 0 R")
    kids_block = " ".join(kids)
    objects.append(f"<< /Type /Pages /Count {len(pages)} /Kids [ {kids_block} ] >>".encode("latin-1"))
    objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

    for page_index, page_lines in enumerate(pages):
        page_obj_id = 4 + page_index * 2
        content_obj_id = page_obj_id + 1

        stream_lines = ["BT", "/F1 10 Tf", "14 TL", "40 805 Td"]
        first = True
        for line in page_lines:
            safe = pdf_escape(line)
            if first:
                stream_lines.append(f"({safe}) Tj")
                first = False
            else:
                stream_lines.append("T*")
                stream_lines.append(f"({safe}) Tj")
        page_number_line = f"Page {page_index + 1}/{len(pages)}"
        stream_lines.append("T*")
        stream_lines.append(f"({pdf_escape(page_number_line)}) Tj")
        stream_lines.append("ET")
        stream = "\n".join(stream_lines).encode("latin-1", errors="replace")

        page_object = (
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
            f"/Resources << /Font << /F1 3 0 R >> >> "
            f"/Contents {content_obj_id} 0 R >>"
        ).encode("latin-1")
        content_object = b"<< /Length " + str(len(stream)).encode("latin-1") + b" >>\nstream\n" + stream + b"\nendstream"

        objects.append(page_object)
        objects.append(content_object)

    pdf = b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n"
    offsets = [0]

    for object_id, obj in enumerate(objects, start=1):
        offsets.append(len(pdf))
        pdf += f"{object_id} 0 obj\n".encode("latin-1")
        pdf += obj + b"\n"
        pdf += b"endobj\n"

    xref_offset = len(pdf)
    pdf += f"xref\n0 {len(objects) + 1}\n".encode("latin-1")
    pdf += b"0000000000 65535 f \n"
    for offset in offsets[1:]:
        pdf += f"{offset:010d} 00000 n \n".encode("latin-1")

    pdf += b"trailer\n"
    pdf += f"<< /Size {len(objects) + 1} /Root 1 0 R >>\n".encode("latin-1")
    pdf += b"startxref\n"
    pdf += f"{xref_offset}\n".encode("latin-1")
    pdf += b"%%EOF"
    return pdf


def main() -> None:
    classes = collect_classes()
    lines = build_document_lines(classes)
    wrapped = wrap_lines(lines)
    pages = paginate(wrapped, page_size=52)
    pdf_bytes = build_pdf_bytes(pages)

    OUTPUT_TXT.write_text("\n".join(wrapped), encoding="utf-8")
    OUTPUT_PDF.write_bytes(pdf_bytes)
    print(f"Documentation texte: {OUTPUT_TXT}")
    print(f"Documentation PDF : {OUTPUT_PDF}")
    print(f"Classes documentees: {len(classes)}")
    print(f"Pages PDF generees : {len(pages)}")


if __name__ == "__main__":
    main()
