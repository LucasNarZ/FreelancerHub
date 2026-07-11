.PHONY: migration

migration:
	@if [ -z "$(name)" ]; then \
		echo "Usage: make migration name=create_users_table"; \
		exit 1; \
	fi
	@./scripts/new-migration.sh "$(name)"
