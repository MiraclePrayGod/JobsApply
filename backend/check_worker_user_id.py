"""
Script para verificar la relación entre users y workers
Ejecutar: python check_worker_user_id.py
"""
import sys
import os
if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    sys.stdout.reconfigure(encoding='utf-8') if hasattr(sys.stdout, 'reconfigure') else None

from app.database import SessionLocal
from app.models import User, Worker

db = SessionLocal()
try:
    print("="*60)
    print("VERIFICACIÓN: Relación Users ↔ Workers")
    print("="*60)
    
    # Buscar el usuario trabajador1@test.com
    user = db.query(User).filter(User.email == "trabajador1@test.com").first()
    
    if user:
        print(f"\n✅ Usuario encontrado:")
        print(f"   ID: {user.id}")
        print(f"   Email: {user.email}")
        print(f"   Role: {user.role}")
        print(f"   Nombre: {user.full_name}")
        
        # Buscar el worker con este user_id
        worker = db.query(Worker).filter(Worker.user_id == user.id).first()
        
        if worker:
            print(f"\n✅ Worker encontrado:")
            print(f"   Worker ID: {worker.id}")
            print(f"   User ID: {worker.user_id}")
            print(f"   Nombre: {worker.full_name}")
            print(f"   Servicios: {worker.services}")
            print(f"   Distrito: {worker.district}")
            print(f"   Verificado: {worker.is_verified}")
            print(f"   Disponible: {worker.is_available}")
            print(f"\n✅ RELACIÓN CORRECTA: user.id={user.id} == worker.user_id={worker.user_id}")
        else:
            print(f"\n❌ PROBLEMA ENCONTRADO:")
            print(f"   No existe un Worker con user_id = {user.id}")
            print(f"\n   Workers existentes:")
            all_workers = db.query(Worker).all()
            for w in all_workers:
                print(f"      - Worker ID: {w.id}, User ID: {w.user_id}, Nombre: {w.full_name}")
    else:
        print(f"\n❌ Usuario trabajador1@test.com no encontrado")
        print(f"\n   Usuarios existentes:")
        all_users = db.query(User).filter(User.role == "worker").all()
        for u in all_users:
            print(f"      - ID: {u.id}, Email: {u.email}, Nombre: {u.full_name}")
    
    print("\n" + "="*60)
    
except Exception as e:
    print(f"❌ Error: {e}")
    import traceback
    traceback.print_exc()
finally:
    db.close()

