#!/usr/bin/env bash
# À sourcer (pas à exécuter) depuis les autres scripts :
#   source "$(dirname "$0")/load-env.sh"
#
# Lit le .env à la racine du projet, exporte toutes les variables KEY=VALUE,
# ignore les lignes vides et les commentaires (#).
# Si .env est absent, le crée depuis .env.example et demande de le compléter.

_env_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
_env_file="$_env_root/.env"

if [ ! -f "$_env_file" ]; then
  if [ -f "$_env_root/.env.example" ]; then
    echo "[env] .env absent — copie de .env.example en .env"
    cp "$_env_root/.env.example" "$_env_file"
    echo "[env] Complétez vos clés API dans .env puis relancez."
  else
    echo "[env] Avertissement : .env introuvable, aucune variable chargée."
  fi
  unset _env_root _env_file
  return 0
fi

echo "[env] Chargement de .env..."
_count=0
while IFS= read -r _line || [ -n "$_line" ]; do
  # Supprimer le retour chariot Windows éventuel
  _line="${_line%$'\r'}"
  # Ignorer les lignes vides et les commentaires
  [[ -z "$_line" || "$_line" =~ ^[[:space:]]*# ]] && continue
  # Exporter uniquement les lignes KEY=VALUE valides
  if [[ "$_line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
    export "$_line"
    (( _count++ ))
  fi
done < "$_env_file"
echo "[env] $_count variable(s) chargée(s)."

unset _env_root _env_file _line _count
